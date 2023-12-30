
package com.ays.theatre.crawler.theatreartbg.worker;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;

public class TheatreArtBgScraperWorkerPool {

    private final TheatreArtBgDayService theatreArtBgDayService;
    private final TheatreArtBgPlayService theatreArtBgPlayService;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final int poolSize;
    private final AtomicInteger workerIdPool;

    public TheatreArtBgScraperWorkerPool(TheatreArtBgDayService theatreArtBgDayService,
                                         TheatreArtBgPlayService theatreArtBgPlayService,
                                         ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                         int poolSize) {
        this.theatreArtBgDayService = theatreArtBgDayService;
        this.theatreArtBgPlayService = theatreArtBgPlayService;
        this.queue = queue;
        this.poolSize = poolSize;
        this.workerIdPool = new AtomicInteger(1);
    }

    public void startWorkers() {
        for (int i = 0; i < poolSize; i++) {
            var t = new Thread(new TheatreArtBgScraperWorker(queue, theatreArtBgDayService,
                    theatreArtBgPlayService, workerIdPool));
            t.start();
        }
    }
}
