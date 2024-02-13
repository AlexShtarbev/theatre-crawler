package com.ays.theatre.crawler.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WorkerPool<W extends WorkerI> {
    private final List<W> workers;
    private final AtomicBoolean started;
    private ExecutorService executorService;

    public WorkerPool() {
        this.workers = new ArrayList<>();
        this.started = new AtomicBoolean(false);
    }

    public void startWorkers(int poolSize) {
        if (started.get()) {
            throw new IllegalCallerException("Workers already started");
        }

        if (executorService != null && !executorService.isTerminated()) {
            throw new IllegalCallerException("Workers already started");
        }

        executorService = Executors.newFixedThreadPool(poolSize);
        started.set(true);

        for (int i = 0; i < poolSize; i++) {
            var worker = getWorker();
            executorService.execute(worker);
            workers.add(worker);
        }
    }

    public void stopWorkers() {
        workers.forEach(WorkerI::interrupt);
        started.set(false);
        workers.clear();
        executorService.close();
    }

    protected abstract W getWorker();

}
