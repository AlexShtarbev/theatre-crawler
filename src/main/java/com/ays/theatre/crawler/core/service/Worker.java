package com.ays.theatre.crawler.core.service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

public abstract class Worker<T> implements WorkerI {
    private final AtomicBoolean running;
    private final ConcurrentLinkedQueue<T> queue;
    private final Logger log;
    private final int id;

    protected Worker(ConcurrentLinkedQueue<T> queue, Logger log, int id) {
        this.queue = queue;
        this.log = log;
        this.id = id;
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        if (running.get()) {
            throw new IllegalStateException(String.format("Thread %d is already started", id));
        }
        log.info(String.format("[%d] Starting worker", id));
        running.set(true);
        while(running.get()) {
            T payload;
            do {
                payload = queue.poll();
            } while (running.get() && payload == null);

            if (payload != null) {
                handlePayload(payload);
            }
        }
        log.info(String.format("Shutting down %d", id));
    }

    public int getId() {
        return id;
    }

    public void interrupt() {
        log.info(String.format("Interrupting %d", id));
        running.getAndSet(false);
    }

    public abstract void handlePayload(T payload);
}
