package com.joy.swiftcrawler.newcrawler;

import com.joy.swiftcrawler.entity.SwiftCode;
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

/**
 * 一个银行的爬取任务，顺序抓取列表，并发爬列表中20个swift code详情页
 */
@Slf4j
public class BankTask implements Runnable {

    private final String bankUrl;

    private final SqlExecutor sqlExecutor;

    private final ExecutorService executor;

    //这个名称是格式是 <country>/<bankName> 国家+银行 才标识一个银行
    private final String bankCountryAndName;

    public BankTask(String bankUrl, SqlExecutor sqlExecutor, int bankConcurrentPageCount) {
        this.bankUrl = bankUrl;
        int slashIdx = bankUrl.lastIndexOf('/');
        slashIdx = bankUrl.lastIndexOf('/', slashIdx - 1);
        int dotIdx = bankUrl.lastIndexOf('.');
        bankCountryAndName = bankUrl.substring(slashIdx + 1, dotIdx);
        this.sqlExecutor = sqlExecutor;
        executor = Executors.newFixedThreadPool(bankConcurrentPageCount, new NamedDaemonThreadFactory("bankSwiftCodeListExecutor"));
    }

    @Override
    public void run() {
        //每个银行的列表页不会太多，就扔到大小20的线程池，多余的任务阻塞等待执行就可以
        try {
            log.info("prepare to get bank[{}] list page count", bankCountryAndName);
            int listCount = getSwiftCodeListCount();
            log.info("get bank[{}] list page count finished, count is[{}]", bankCountryAndName, listCount);
            for (int i = 1; i <= listCount; i++) {
                final int idx = i;
                executor.execute(() -> processPage(idx));
            }
            executor.shutdown();
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            log.info("finish process bank[{}]", bankCountryAndName);
        } catch (Exception e) {
            log.error("process bank[{}] fail!", bankUrl, e);
        }
    }

    private void processPage(int idx) {
        log.info("start process page[{}]", swiftCodeListPageUrl(idx));
        //一个线程用一个httpclient就好了
        CloseableHttpClient httpClient = HttpClients.createMinimal();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            List<String> swiftCodeUrls = getSwiftCodeUrls(idx, httpClient);
            for (String swiftCodeUrl : swiftCodeUrls) {
                processSwiftCode(swiftCodeUrl, httpClient);
            }
            log.info("finish process page[{}]", swiftCodeListPageUrl(idx));
        } catch (Exception e) {
            log.error("process bank[{}] page[{}] fail!", bankUrl, idx, e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                //noop
            }
        }
    }

    //把sql投递到异步队列，批量插入
    private void processSwiftCode(String swiftCodeUrl, CloseableHttpClient httpClient) {
        int retry = 0;
        while (true) {
            try {
                HttpGet get = HttpClientUtil.newGet(swiftCodeUrl);
                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Document document = Jsoup.parse(html);
                    Element table = SwiftCodeUtil.selectTableElement(document);
                    SwiftCode swiftCode = SwiftCodeUtil.getSwiftCode(table);
                    if (swiftCode != null) {
                        sqlExecutor.addSwiftCode(swiftCode);
                    }
                }
                break;
            } catch (Exception e) {
                retry++;
                if (retry <= 3) {
                    log.error("process swiftCode[{}] error! will retry later!", swiftCodeUrl, e);
                } else {
                    log.error("process swiftCode[{}] fail!please check network stat!", swiftCodeUrl, e);
                    break;
                }
            }
        }
    }

    //获取某一页所有的swiftCode详情url地址
    private List<String> getSwiftCodeUrls(int idx, CloseableHttpClient httpClient) throws Exception {
        List<String> swiftCodeUrls = new ArrayList<>();
        int retry = 0;
        while (true) {
            swiftCodeUrls.clear();
            try {
                HttpGet get = HttpClientUtil.newGet(swiftCodeListPageUrl(idx));
                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Document document = Jsoup.parse(html);
                    Element table = SwiftCodeUtil.selectTableElement(document);
                    Element tbody = table.getElementsByTag("tbody").get(0);
                    Elements trs = tbody.getElementsByTag("tr");
                    for (int i = 1; i < trs.size(); i++) {
                        String swiftCodeUrl = trs.get(i).getElementsByTag("td")
                                .get(4).getElementsByTag("a").get(0).attr("href");
                        swiftCodeUrls.add(swiftCodeUrl);
                    }
                    return swiftCodeUrls;
                }
            } catch (Exception e) {
                retry++;
                if (retry <= 3) {
                    log.error("get bank[{}] swift code list count error! will try again!", bankUrl, e);
                } else {
                    log.error("get bank[{}] swift code list count fail! please check network stat!", bankUrl, e);
                    throw e;
                }
            }
        }
    }

    private String swiftCodeListPageUrl(int idx) {
        int dotIdx = bankUrl.lastIndexOf('.');
        String prefix = bankUrl.substring(0, dotIdx);
        return prefix + "-" + idx + ".html";
    }

    private int getSwiftCodeListCount() throws Exception {
        int retry = 0;
        while (true) {
            try {
                try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
                    HttpGet get = HttpClientUtil.newGet(bankUrl);
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        Document document = Jsoup.parse(html);
                        return SwiftCodeUtil.getPageCount(document);
                    }
                }
            } catch (Exception e) {
                retry++;
                if (retry <= 3) {
                    log.error("get bank[{}] swift code list count error! will try again!", bankUrl, e);
                } else {
                    log.error("get bank[{}] swift code list count fail! please check network stat!", bankUrl, e);
                    throw e;
                }
            }
        }
    }
}
