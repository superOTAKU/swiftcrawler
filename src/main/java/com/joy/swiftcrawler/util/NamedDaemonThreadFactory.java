package com.joy.swiftcrawler.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedDaemonThreadFactory implements ThreadFactory {

    private final AtomicInteger id = new AtomicInteger(0);

    private final String name;

    public NamedDaemonThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setPriority(Thread.NORM_PRIORITY);
        t.setDaemon(true);
        t.setName(name + "-" + id.incrementAndGet());
        return t;
    }
}
