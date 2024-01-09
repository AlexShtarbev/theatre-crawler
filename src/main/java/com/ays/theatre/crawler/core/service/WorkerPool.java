package com.ays.theatre.crawler.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WorkerPool<W extends WorkerI> {
    private final int poolSize;
    private final List<W> workers;
    private final AtomicBoolean started;
    private final AtomicBoolean stopped;

    public WorkerPool(int poolSize) {
        this.poolSize = poolSize;
        this.workers = new ArrayList<>();
        this.started = new AtomicBoolean(false);
        this.stopped = new AtomicBoolean(false);
    }

    public void startWorkers() {
        if (stopped.get()) {
            throw new IllegalCallerException("Workers already stopped");
        }
        if (started.get()) {
            throw new IllegalCallerException("Workers already started");
        }

        started.set(true);
        stopped.set(false);
        for (int i = 0; i < poolSize; i++) {
            var worker = getWorker();
            var t = new Thread(worker);
            workers.add(worker);
            t.start();
        }
    }

    public void stopWorkers() {
        workers.forEach(WorkerI::interrupt);
        stopped.set(true);
        started.set(false);
    }

    protected abstract W getWorker();

}
