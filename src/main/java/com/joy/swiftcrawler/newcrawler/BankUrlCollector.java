package com.joy.swiftcrawler.newcrawler;

import com.joy.swiftcrawler.util.HttpClientUtil;
import com.joy.swiftcrawler.util.NamedDaemonThreadFactory;
import com.joy.swiftcrawler.util.SwiftCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.joy.swiftcrawler.config.Constants.BASE_CRAWLER_URL;

/**
 * 收集所有的银行列表url
 */
@Slf4j
public class BankUrlCollector {
    private static final int THREAD_COUNT = 50;
    private final List<String> bankUrls = new ArrayList<>();

    public List<String> getAllBankUrls() throws Exception {
        log.info("prepare to get bankListPageCount!");
        int pageCount = getBankListPageCount();
        log.info("bankListPageCount is [{}], prepare to get all BankUrls.", pageCount);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, new NamedDaemonThreadFactory("getBankUrlsExecutor"));
        for (int page = 1; page <= pageCount; page++) {
            final int curPage = page;
            executor.execute(() -> {
                try {
                    processPage(curPage);
                } catch (Exception e) {
                    log.error("get bankUrls from page[" + curPage + "] fail!", e);
                }
            });
        }
        executor.shutdown();
        long startTime = System.nanoTime();
        //等待的时间足够长！
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        long timeCost = System.nanoTime() - startTime;
        log.info("get all bank urls finished!time cost is {} seconds!", TimeUnit.NANOSECONDS.toSeconds(timeCost));
        return new ArrayList<>(bankUrls);
    }

    private void processPage(int page) throws Exception {
        List<String> bankUrls = new ArrayList<>();
        int retry = 0;
        while (true) {
            bankUrls.clear();
            try {
                try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
                    HttpGet get = HttpClientUtil.newGet(BASE_CRAWLER_URL + "banks-" + page + ".html");
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        Document document = Jsoup.parse(html);
                        Element table = getTableElement(document);
                        Element tbody = table.getElementsByTag("tbody").get(0);
                        Elements trs = tbody.getElementsByTag("tr");
                        for (int i = 1; i < trs.size(); i++) {
                            String bankUrl = trs.get(i).getElementsByTag("td")
                                    .get(2).getElementsByTag("a").get(0).attr("href");
                            bankUrls.add(bankUrl);
                        }
                        //跳出重试while
                        break;
                    }
                }
            } catch (Exception e) {
                retry++;
                if (retry <= 3) {
                    log.error("get bankUrls from page[" + page + "] error, will try again!", e);
                } else {
                    log.error("get bankUrls from page[" + page + "] error, please check network stat!", e);
                    throw e;
                }
            }
        }
        log.info("get bankUrls from page[{}] success!", page);
        synchronized (this) {
            this.bankUrls.addAll(bankUrls);
        }
    }

    private Element getTableElement(Document document) {
        return SwiftCodeUtil.selectTableElement(document);
    }

    private int getBankListPageCount() throws Exception {
        int retry = 0;
        while (true) {
            try {
                try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
                    HttpGet get = HttpClientUtil.newGet(BASE_CRAWLER_URL + "banks.html");
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        Document document = Jsoup.parse(html);
                        return SwiftCodeUtil.getPageCount(document);
                    }
                }
            } catch (Exception e) {
                retry++;
                if (retry <= 3) {
                    log.error("getBankListPageCount error, will try again!", e);
                } else {
                    log.error("getBankListPageCount fail! please check network stat!", e);
                    throw e;
                }
            }
        }
    }

}
