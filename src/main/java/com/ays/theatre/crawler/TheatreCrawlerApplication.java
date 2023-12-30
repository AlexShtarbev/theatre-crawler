package com.ays.theatre.crawler;

import com.ays.theatre.crawler.calendar.GoogleCalendarEventSchedulerWorker;
import com.ays.theatre.crawler.calendar.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(TheatreCrawlerApplication.class);

    private static final int PARALLEL_WORKERS_SIZE = 20;

    @Inject
    TheatreArtBgDayService theatreArtBgDayService;

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

    // FIXME remove
    @Inject
    TheatreArtBgPlayService theatreArtBgPlayService;

    @Override
    public int run(String... args) {
//        var url = "https://theatre.art.bg/разговори-с-мама_3550_8_20";
//        theatreArtBgPlayService.scrape(ImmutableTheatreArtBgPlayObject.builder().build(), url);
        //
        //        Thread.ofVirtual().start(googleCalendarEventSchedulerWorker);
        //
        //        calendarQueue.add(ImmutableGoogleCalendarEventSchedulerPayload.builder()
        //                                  .title("БЛАЖЕНИ СА БЛАЖЕНИТЕ")
        //                                  .theatre("ТЕАТЪР 199 \"ВАЛЕНТИН СТОЙЧЕВ\"")
        //                                  .url("<a href=\"https://theatre.art.bg/блажени-са-блажените_7189_8_20\">link</a><br/>"
        //                                       + "<img src=\"https://theatre.peakview.bg/theatre/photos/INDEX16999993771DSC_4112%20(2).jpg\" alt=\"Italian Trulli\"/>")
        //                                  .startTime(OffsetDateTime.now())
        //                                  .build());
//
        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgDayService, theatreArtBgPlayService, queue,
                                                           PARALLEL_WORKERS_SIZE);
        workerPool.startWorkers();
        theatreArtBgJob.run();

        return 0;
    }
}
