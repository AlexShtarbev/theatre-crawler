package com.ays.theatre.crawler;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarEventSchedulerWorker;
import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.calendar.resync.GoogleCalendarReSyncService;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GoogleCalendarReSyncService googleCalendarReSyncService;

    @Override
    public int run(String... args) throws JsonProcessingException {
//        var url = "https://theatre.art.bg/разговори-с-мама_3550_8_20";
//        var url = "https://theatre.art.bg/внимание!-любов!_7120_7_20";
//        var url = "https://theatre.art.bg/хотел-между-тоя-и-оня-свят_6373_6_20";
//        var dayUrl = "https://theatre.art.bg/%D1%82%D0%B5%D0%B0%D1%82%D1%80%D0%B8-%D1%81%D0%BE%D1%84%D0%B8%D1%8F-%D0%BF%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%B0______2024-01-16______20";
//        latchService.init(Constants.THEATRE_ART_BG_DAY_LATCH, 1);
//        theatreArtBgDayService.scrape(ImmutableTheatreArtBgCalendar.builder().month(1).year(2024).url(dayUrl).build(),dayUrl);
//        latchService.await(Constants.THEATRE_ART_BG_DAY_LATCH);
//
//        var url = "https://theatre.art.bg/по-полека_5303_4_20";
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

//        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgDayService, theatreArtBgPlayService, queue,
//                                                           PARALLEL_WORKERS_SIZE);
//        workerPool.startWorkers();
//        theatreArtBgJob.run();

        final var url = "https://theatre.art.bg/по-полека_5303_4_20";
//        final var time = OffsetDateTime.parse("2023-12-30T21:00:00+02:00");
        final var time = OffsetDateTime.parse("2024-01-16T21:00:00+02:00");
        latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, 1);
        theatreArtBgPlayService.scrape(ImmutableTheatreArtBgPlayObject.builder().build(), url);
        Optional<TheatrePlayRecord>  play = theatrePlayDao.getPlayFromUrlAndDate(url, time);
        Optional<TheatrePlayDetailsRecord> maybeDetails = theatrePlayDao.getTheatrePlayDetails(url);

        maybeDetails.ifPresent(playRecord -> {
            var payload = getEventSchedulerPayload(playRecord, play.get());
            var event = googleCalendarService.createCalendarEvent(payload);
            var createdEvent = googleCalendarService.getEventById(event.getId());
            googleCalendarReSyncService.reSyncEvent(event, Constants.THEATRE_ART_BG_ORIGIN);
        });
        return 0;
    }

    private static ImmutableGoogleCalendarEventSchedulerPayload getEventSchedulerPayload(
            TheatrePlayDetailsRecord playRecord,
            TheatrePlayRecord play) {
        return ImmutableGoogleCalendarEventSchedulerPayload.builder()
                .title(play.getTitle())
                .theatre(play.getTheatre())
                .startTime(OffsetDateTime.now()) // play.get().getDate()
                .url(playRecord.getUrl())
                .theatreArtBgTicket(play.getTicketsUrl())
                .crew(playRecord.getCrew())
                .description(playRecord.getDescription())
                .rating(playRecord.getRating())
                .lastUpdated(play.getLastUpdated())
                .build();
    }
}
