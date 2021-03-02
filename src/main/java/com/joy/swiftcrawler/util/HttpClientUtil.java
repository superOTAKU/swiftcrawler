package com.joy.swiftcrawler.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

public class HttpClientUtil {

    public static HttpGet newGet(String url) {
        HttpGet get = new HttpGet(url);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        get.setConfig(config);
        return get;
    }

}
