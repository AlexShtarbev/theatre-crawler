
package com.ays.theatre.crawler.theatreartbg.worker;

import static com.ays.theatre.crawler.Configuration.THEATRE_ART_BG_WORKER_POOL_SIZE;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.WorkerI;
import com.ays.theatre.crawler.core.service.WorkerPool;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgScraperWorkerPool extends WorkerPool<TheatreArtBgScraperWorker> {

    private final TheatreArtBgDayService theatreArtBgDayService;
    private final TheatreArtBgPlayService theatreArtBgPlayService;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final AtomicInteger workerIdPool;

    public TheatreArtBgScraperWorkerPool(TheatreArtBgDayService theatreArtBgDayService,
                                         TheatreArtBgPlayService theatreArtBgPlayService,
                                         ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                         @Named(THEATRE_ART_BG_WORKER_POOL_SIZE) int poolSize) {
        super(poolSize);
        this.theatreArtBgDayService = theatreArtBgDayService;
        this.theatreArtBgPlayService = theatreArtBgPlayService;
        this.queue = queue;
        this.workerIdPool = new AtomicInteger(1);
    }

    @Override
    protected TheatreArtBgScraperWorker getWorker() {
        return new TheatreArtBgScraperWorker(queue, theatreArtBgDayService, theatreArtBgPlayService, workerIdPool);
    }
}
