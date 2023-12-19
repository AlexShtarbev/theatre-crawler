package com.ays.theatre.crawler;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    private static final int PARALLEL_WORKERS_SIZE = 5;

    @Inject
    TheatreArtBgScraperService theatreArtBgScraperService;

    @Inject
    TheatreArtBgJob theatreArtBgJob;

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    @Inject
    TheatrePlayDao theatrePlayDao;

    @Override
    public int run(String... args) throws Exception {
        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgScraperService, queue, theatrePlayDao,
                                                           PARALLEL_WORKERS_SIZE);
        workerPool.startWorkers();
        theatreArtBgJob.run();
        return 0;
    }
}
