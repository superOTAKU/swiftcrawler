package com.joy.swiftcrawler.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class CrawlerConfig {
    //同时并发处理的银行数量
    @Value("${crawler.concurrentBankCount}")
    private int concurrentBankCount;
    //每个银行并发处理的页数
    @Value("${crawler.bankConcurrentPageCount}")
    private int bankConcurrentPageCount;
    //批量执行sql的线程数
    @Value("${crawler.sqlConcurrentCount}")
    private int sqlConcurrentCount;
    //每个sql线程休眠的毫秒数
    @Value("${crawler.sqlSleepIntervalMillis}")
    private int sqlSleepIntervalMillis;
    //一次最多批量多少个insert
    @Value("${crawler.sqlBatchSize}")
    private int sqlBatchSize;
}
