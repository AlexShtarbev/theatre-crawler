package com.ays.theatre.crawler;

import java.time.OffsetDateTime;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.resync.GoogleCalendarReSyncService;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

//@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    private final TheatreArtBgRunner theatreArtBgJob;
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarReSyncService googleCalendarReSyncService;

    private final TheatreArtBgPlayService theatreArtBgPlayService;
    private final LatchService latchService;

    public TheatreCrawlerApplication(TheatreArtBgRunner theatreArtBgJob,
                                     GoogleCalendarService googleCalendarService,
                                     GoogleCalendarReSyncService googleCalendarReSyncService,
                                     TheatreArtBgPlayService theatreArtBgPlayService,
                                     LatchService latchService) {
        this.theatreArtBgJob = theatreArtBgJob;
        this.googleCalendarService = googleCalendarService;
        this.googleCalendarReSyncService = googleCalendarReSyncService;
        this.theatreArtBgPlayService = theatreArtBgPlayService;
        this.latchService = latchService;
    }

    @Override
    public int run(String... args) {
//        var allEvents = googleCalendarService.getAllEvents(OffsetDateTime.now());
//        googleCalendarService.delete(allEvents);
//
//        googleCalendarReSyncService.reSync();
//
        theatreArtBgJob.run();


//        latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, 1);
//        theatreArtBgPlayService.scrape(ImmutableTheatreArtBgPlayObject.builder().build(),
//                                       "https://theatre.art.bg/урок-по-български_5876_6_20",
//                                       OffsetDateTime.now());
//        latchService.await(Constants.THEATRE_ART_BG_PLAY_LATCH);

        return 0;
    }
}
