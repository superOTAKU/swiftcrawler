package com.joy.swiftcrawler;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.joy.swiftcrawler.newcrawler.SwiftCodeCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication(exclude = DruidDataSourceAutoConfigure.class)
public class SwiftCrawlerApplication {

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext ctx = SpringApplication.run(SwiftCrawlerApplication.class, args);
            SwiftCodeCrawler swiftCodeCrawler = ctx.getBean(SwiftCodeCrawler.class);
            swiftCodeCrawler.start();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("exception in main!", e);
        }
    }

}
