package com.joy.swiftcrawler.crawler;

import com.joy.swiftcrawler.entity.SwiftCode;
import com.joy.swiftcrawler.service.SwiftCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
public class CrawlerHelper {

    //银行列表页
    private final static Pattern BANK_LIST =
            Pattern.compile("https://www\\.swiftcodelist\\.com/banks(\\.html|-[0-9]+\\.html)");

    //某个银行的swift code列表页url模式
    //例子：https://www.swiftcodelist.com/bank/china/bank_of_china-2.html
    private final static Pattern BANK_SWIFT_CODE_LIST_PATTERN =
            Pattern.compile("https://www\\.swiftcodelist\\.com/bank/[a-zA-Z_0-9]+/[a-zA-Z_0-9]+\\.html");

    //一个具体的swift code 详情页url
    private final static Pattern SWIFT_CODE_PAGE_PATTERN =
            Pattern.compile("https://www\\.swiftcodelist\\.com/swift-code/[A-Z0-9]+\\.html");


    /**
     * 银行列表页是否访问过
     */
    private final Set<String> bankListSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 银行swiftCode列表页是否访问过
     */
    private final Set<String> bankSwiftCodeListSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * swiftCode是否爬取过
     */
    private final Set<String> swiftCodeSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final SwiftCodeService swiftCodeService;

    @Autowired
    public CrawlerHelper(SwiftCodeService swiftCodeService) {
        this.swiftCodeService = swiftCodeService;
    }

    /**
     * 基于url模式预先过滤
     */
    public boolean patternFilter(String url) {
        if (BANK_LIST.matcher(url).matches()) {
            log.debug("match bank list[{}]", url);
            return true;
        }
        if (BANK_SWIFT_CODE_LIST_PATTERN.matcher(url).matches()) {
            log.debug("match bank swift code list[{}]", url);
            return true;
        }
        if (SWIFT_CODE_PAGE_PATTERN.matcher(url).matches()) {
            log.debug("match swift code [{}]", url);
            return true;
        }
        return false;
    }

    /**
     * 是否需要爬虫，没有爬过才去爬取
     */
    public boolean preVisitFilter(String url) {
        if (BANK_LIST.matcher(url).matches()) {
            if (bankListSet.contains(url)) {
                return false;
            } else {
                return bankListSet.add(url);
            }
        }
        if (BANK_SWIFT_CODE_LIST_PATTERN.matcher(url).matches()) {
            if (bankSwiftCodeListSet.contains(url)) {
                return false;
            } else {
                return bankSwiftCodeListSet.add(url);
            }
        }
        if (SWIFT_CODE_PAGE_PATTERN.matcher(url).matches()) {
            if (swiftCodeSet.contains(url)) {
                return false;
            } else {
                return swiftCodeSet.add(url);
            }
        }
        return false;
    }

    public boolean isSwiftCodePage(String url) {
        return SWIFT_CODE_PAGE_PATTERN.matcher(url).matches();
    }

    public void saveSwiftCode(SwiftCode swiftCode) {
        log.debug("save swift code[{}]", swiftCode.getSwiftCode());
        swiftCodeService.saveOrUpdate(swiftCode);
    }

}
