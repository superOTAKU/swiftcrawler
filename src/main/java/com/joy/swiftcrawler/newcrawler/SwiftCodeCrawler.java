package com.joy.swiftcrawler.newcrawler;

import com.joy.swiftcrawler.config.CrawlerConfig;
import com.joy.swiftcrawler.util.NamedDaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

/**
 * 使用httpclient做爬虫，方案：
 * 1.并发爬取10个银行
 * 2.每个银行并发20个线程爬取
 * 3.爬完1个银行，再爬下一个银行
 */
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Component
public class SwiftCodeCrawler implements InitializingBean {

    private ExecutorService executor;

    @Autowired
    private SqlExecutor sqlExecutor;

    @Autowired
    private CrawlerConfig config;

    //开始爬虫
    public void start() throws Exception {
        List<String> allBankUrls = getAllBankUrls();
        for (var bankUrl : allBankUrls) {
            log.info("bank[{}] start crawl task!", bankUrl);
            executor.submit(new BankTask(bankUrl, sqlExecutor, config.getBankConcurrentPageCount()), bankUrl);
        }
        executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        log.info("all bank crawl finish! congratulations!");
        sqlExecutor.shutDown();
        log.info("good bye!");
    }

    /**
     * 获取所有的银行名称，考虑开50个线程，并发获取名称，最后放到一个统一的列表中
     */
    private List<String> getAllBankUrls() throws Exception {
        return new BankUrlCollector().getAllBankUrls();
    }

    @Override
    public void afterPropertiesSet() {
        int concurrentBankCount = config.getConcurrentBankCount();
        //10个线程的线程池，不接受多余的任务
        executor = new ThreadPoolExecutor(concurrentBankCount, concurrentBankCount,
                0L, TimeUnit.MINUTES, new SynchronousQueue<>(),
                new NamedDaemonThreadFactory("swiftCodeCrawler"), new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
