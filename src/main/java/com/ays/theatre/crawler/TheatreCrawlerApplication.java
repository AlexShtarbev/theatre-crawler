package com.ays.theatre.crawler;

import java.time.OffsetDateTime;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.resync.GoogleCalendarReSyncService;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

//@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    private final TheatreArtBgRunner theatreArtBgJob;
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarReSyncService googleCalendarReSyncService;

    public TheatreCrawlerApplication(TheatreArtBgRunner theatreArtBgJob,
                                     GoogleCalendarService googleCalendarService,
                                     GoogleCalendarReSyncService googleCalendarReSyncService) {
        this.theatreArtBgJob = theatreArtBgJob;
        this.googleCalendarService = googleCalendarService;
        this.googleCalendarReSyncService = googleCalendarReSyncService;
    }

    @Override
    public int run(String... args) {
        var allEvents = googleCalendarService.getAllEvents(OffsetDateTime.now());
        googleCalendarService.delete(allEvents);

        googleCalendarReSyncService.reSync();

        theatreArtBgJob.run();

        return 0;
    }
}
