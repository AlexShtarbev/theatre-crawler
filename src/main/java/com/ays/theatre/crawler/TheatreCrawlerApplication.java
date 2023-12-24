package com.ays.theatre.crawler;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ays.theatre.crawler.calendar.GoogleCalendarEventSchedulerWorker;
import com.ays.theatre.crawler.calendar.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;
import com.ays.theatre.crawler.utils.DateUtils;

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

    @Inject
    GoogleCalendarService googleCalendarService;

    @Inject
    GoogleCalendarEventSchedulerWorker googleCalendarEventSchedulerWorker;

    // TODO - remove
    @Inject
    ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> calendarQueue;

    @Override
    public int run(String... args) {
        Thread.ofVirtual().start(googleCalendarEventSchedulerWorker);

        calendarQueue.add(ImmutableGoogleCalendarEventSchedulerPayload.builder()
                                  .title("БЛАЖЕНИ СА БЛАЖЕНИТЕ")
                                  .theatre("ТЕАТЪР 199 \"ВАЛЕНТИН СТОЙЧЕВ\"")
                                  .url("https://theatre.art.bg/блажени-са-блажените_7189_8_20")
                                  .startTime(OffsetDateTime.now())
                                  .build());

//        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgScraperService, queue, theatrePlayDao,
//                                                           PARALLEL_WORKERS_SIZE);
//        workerPool.startWorkers();
//        theatreArtBgJob.run();
        return 0;
    }
}
