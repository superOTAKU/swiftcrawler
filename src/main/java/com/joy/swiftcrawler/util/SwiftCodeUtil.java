package com.joy.swiftcrawler.util;

import com.joy.swiftcrawler.entity.SwiftCode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SwiftCodeUtil {

    @SuppressWarnings("SpellCheckingInspection")
    public static Element selectTableElement(Document document) {
        Elements tables = document.getElementsByTag("table");
        Element table = null;
        for (Element t : tables) {
            if (t.hasClass("pro") && t.hasClass("magt10")) {
                table = t;
                break;
            }
        }
        if (table == null) {
            throw new IllegalStateException("no table found!");
        }
        return table;
    }

    public static int getPageCount(Document document) {
        Element pageNav = document.getElementsByClass("pageNav").get(0);
        String text = pageNav.text();
        int ofIdx = text.indexOf("Of");
        text = text.substring(ofIdx + 2).trim();
        text = text.substring(0, text.indexOf(" "));
        return Integer.parseInt(text);
    }

    public static SwiftCode getSwiftCode(Element table) {
        Element tbody = table.getElementsByTag("tbody").get(0);
        Elements trs = tbody.getElementsByTag("tr");
        SwiftCode.SwiftCodeBuilder builder = SwiftCode.builder();
        for (var tr : trs) {
            Elements tds = tr.getElementsByTag("td");
            String title = tds.get(0).getElementsByTag("strong").get(0).text().trim();
            String value = tds.get(1).text().trim();
            switch (title) {
                case "Swift Code":
                    builder.swiftCode(value);
                    break;
                case "Country":
                    builder.country(value);
                    break;
                case "Bank":
                    builder.bank(value);
                    break;
                case "Branch":
                    if (value.startsWith("(")) value = value.substring(1);
                    if (value.endsWith(")")) value = value.substring(0, value.length() - 1);
                    builder.branch(value);
                    break;
                case "City":
                    builder.city(value);
                    break;
                case "Zipcode":
                    builder.zipcode(value);
                    break;
                case "Address":
                    builder.address(value);
                    break;
            }
        }
        return builder.build();
    }

}
