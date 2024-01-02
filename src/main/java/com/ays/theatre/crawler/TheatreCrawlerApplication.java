package com.ays.theatre.crawler;

import com.ays.theatre.crawler.calendar.GoogleCalendarDescriptionFormatter;
import com.ays.theatre.crawler.calendar.GoogleCalendarEventSchedulerWorker;
import com.ays.theatre.crawler.calendar.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.global.Constants;
import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.LatchService;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
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

import java.time.OffsetDateTime;
import java.util.Optional;
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

    @Inject
    LatchService latchService;

    @Override
    public int run(String... args) {
//        var url = "https://theatre.art.bg/разговори-с-мама_3550_8_20";
//        var url = "https://theatre.art.bg/внимание!-любов!_7120_7_20";
//        var url = "https://theatre.art.bg/хотел-между-тоя-и-оня-свят_6373_6_20";
//        latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, 1);
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

        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgDayService, theatreArtBgPlayService, queue,
                                                           PARALLEL_WORKERS_SIZE);
        workerPool.startWorkers();
        theatreArtBgJob.run();

//        final var url = "https://theatre.art.bg/червено-и-черно_6771_3_20";
//        final var time = OffsetDateTime.parse("2023-12-30T21:00:00+02:00");
//        theatreArtBgPlayService.scrape(ImmutableTheatreArtBgPlayObject.builder().build(), url);
//        Optional<TheatrePlayRecord>  play = theatrePlayDao.getPlayFromUrlAndDate(url, time);
//        Optional<TheatrePlayDetailsRecord> maybeDetails = theatrePlayDao.getTheatrePlayDetails("https://theatre.art.bg/червено-и-черно_6771_3_20");
//        maybeDetails.ifPresent(r -> {
//            var payload = ImmutableGoogleCalendarEventSchedulerPayload.builder()
//                    .title(play.get().getTitle())
//                    .theatre(play.get().getTheatre())
//                    .startTime(play.get().getDate())
//                    .url(r.getUrl())
//                    .crew(r.getCrew())
//                    .description(r.getDescription())
//                    .rating(r.getRating())
//                    .build();
//            var eventDescription = GoogleCalendarDescriptionFormatter.getHtmlEventDescription(payload).replace("<br/>", "");
//            googleCalendarService.createCalendarEvent(payload.getTitle(), payload.getTheatre(),
//                    eventDescription, payload.getUrl(), OffsetDateTime.now());
//        });
        return 0;
    }
}
