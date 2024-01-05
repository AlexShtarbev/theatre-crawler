package com.ays.theatre.crawler.core.service;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Singleton
public class LatchService {

    private final Map<String, CountDownLatch> latches = new ConcurrentHashMap<>();

    public void init(String name, int size) {
        var latch = latches.get(name);
        if (latch != null) {
            throw new IllegalStateException("Countdown latch is already initialized");
        }
        latches.put(name, new CountDownLatch(size));
    }

    public void countDown(String name) {
        var latch = getLatch(name);
        latch.countDown();;
    }

    public void await(String name) {
        try {
            var latch = getLatch(name);
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void reset() {
        var keys = latches.keySet();
        keys.forEach(latches::remove);
    }

    private CountDownLatch getLatch(String name) {
        var latch = latches.get(name);
        if (latch == null) {
            throw new IllegalArgumentException(String.format("Countdown latch %s is not initialized", name));
        }
        return latch;
    }
}
