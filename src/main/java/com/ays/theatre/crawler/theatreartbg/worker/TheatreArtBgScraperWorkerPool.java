/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.worker;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;

public class TheatreArtBgScraperWorkerPool {

    private final TheatreArtBgScraperService theatreArtBgScraperService;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final int poolSize;

    public TheatreArtBgScraperWorkerPool(TheatreArtBgScraperService theatreArtBgScraperService,
                                         ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                         int poolSize) {
        this.theatreArtBgScraperService = theatreArtBgScraperService;
        this.queue = queue;
        this.poolSize = poolSize;
    }

    public void startWorkers() {
        for (int i = 0; i < poolSize; i++) {
            Thread.ofVirtual().start(new TheatreArtBgScraperWorker(theatreArtBgScraperService, queue));
        }
    }
}
