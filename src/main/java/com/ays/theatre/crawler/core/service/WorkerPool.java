package com.ays.theatre.crawler.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WorkerPool<W extends WorkerI> {
    private final List<W> workers;
    private final AtomicBoolean started;

    public WorkerPool() {
        this.workers = new ArrayList<>();
        this.started = new AtomicBoolean(false);
    }

    public void startWorkers(int poolSize) {
        if (started.get()) {
            throw new IllegalCallerException("Workers already started");
        }

        started.set(true);
        for (int i = 0; i < poolSize; i++) {
            var worker = getWorker();
            var t = new Thread(worker);
            workers.add(worker);
            t.start();
        }
    }

    public void stopWorkers() {
        workers.forEach(WorkerI::interrupt);
        started.set(false);
        workers.clear();
    }

    protected abstract W getWorker();

}
