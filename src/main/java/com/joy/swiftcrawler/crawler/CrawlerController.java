package com.joy.swiftcrawler.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;

@Slf4j
public class CrawlerController implements InitializingBean {

    private final CrawlerHelper helper;

    @Value("${swift-crawler.folder}")
    private String crawlerFolder;

    @Value("${swift-crawler.start-page}")
    private String startPage;

    @Autowired
    public CrawlerController(CrawlerHelper helper) {
        this.helper = helper;
    }

    public void startCrawler() throws Exception {
        CrawlConfig config = new CrawlConfig();
        //如果有100页，那么从第1页走到第2页，第2页走到第3页，深度可能很深...
        config.setMaxDepthOfCrawling(400);
        config.setCrawlStorageFolder(crawlerFolder);
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed(startPage);
        CrawlController.WebCrawlerFactory<SwiftCodeCrawler> factory = () -> new SwiftCodeCrawler(helper);
        //IO线程数可以比较多
        controller.start(factory, 50);
        controller.shutdown();
        log.info("swift code crawl end!");
    }

    @Override
    public void afterPropertiesSet() {
        Objects.requireNonNull(crawlerFolder, "crawlerFolder can't be null!");
        Objects.requireNonNull(startPage, "startPage can't be null!");
        log.info("crawler controller init! crawlerFolder[{}], startPage[{}]", crawlerFolder, startPage);
    }
}
