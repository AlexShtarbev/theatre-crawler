
package com.ays.theatre.crawler.theatreartbg.worker;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;

public class TheatreArtBgScraperWorkerPool {

    private final TheatreArtBgScraperService theatreArtBgScraperService;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final TheatrePlayDao theatrePlayDao;
    private final int poolSize;

    public TheatreArtBgScraperWorkerPool(TheatreArtBgScraperService theatreArtBgScraperService,
                                         ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                         TheatrePlayDao theatrePlayDao,
                                         int poolSize) {
        this.theatreArtBgScraperService = theatreArtBgScraperService;
        this.queue = queue;
        this.poolSize = poolSize;
        this.theatrePlayDao = theatrePlayDao;
    }

    public void startWorkers() {
        for (int i = 0; i < poolSize; i++) {
            Thread.ofVirtual().start(new TheatreArtBgScraperWorker(theatreArtBgScraperService, queue, theatrePlayDao));
        }
    }
}
