package com.joy.swiftcrawler.crawler;

import com.joy.swiftcrawler.entity.SwiftCode;
import com.joy.swiftcrawler.util.SwiftCodeUtil;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

/**
 *
 * 从https://www.swiftcodelist.com/爬取swift code，保存到数据库
 *
 * @author linjingfu
 */
@Slf4j
public class SwiftCodeCrawler extends WebCrawler {
    private final CrawlerHelper helper;

    public SwiftCodeCrawler(CrawlerHelper helper) {
        this.helper = helper;
    }

    /**
     * 访问逻辑：
     * 1. 访问银行列表页
     * 2. 访问银行分支列表
     * 3. 进入分支详情，保存数据
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return helper.patternFilter(url.getURL());
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        if (!helper.preVisitFilter(url)) {
            log.info("Duplicate Visited URL [{}]", page.getWebURL().getURL());
            return;
        }
        //只有swift code详情页才进行处理
        if (!helper.isSwiftCodePage(url)) {
            return;
        }
        try {
            Optional<SwiftCode> swiftCode = crawlSwiftCodeFromPage(page);
            swiftCode.ifPresent(helper::saveSwiftCode);
        } catch (Exception e) {
            log.error("crawl swift code page error", e);
        }
    }

    private Optional<SwiftCode> crawlSwiftCodeFromPage(Page page) {
        HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
        String html = htmlParseData.getHtml();
        Document document = Jsoup.parse(html);
        Elements tables = document.getElementsByTag("table");
        Element table = null;
        for (var t : tables) {
            if (t.hasClass("pro") && t.hasClass("magt10")) {
                table = t;
                break;
            }
        }
        if (table == null) {
            log.warn("swift code page no table found!");
            return Optional.empty();
        }
        SwiftCode swiftCode = SwiftCodeUtil.getSwiftCode(table);
        return Optional.of(swiftCode);
    }

}
