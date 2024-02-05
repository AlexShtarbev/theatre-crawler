package com.ays.theatre.crawler;

import java.time.OffsetDateTime;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;

import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;

//@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    @Inject
    TheatreArtBgRunner theatreArtBgJob;

    @Inject
    GoogleCalendarService googleCalendarService;

    @Override
    public int run(String... args) {
        var allEvents = googleCalendarService.getAllEvents(OffsetDateTime.now());
        googleCalendarService.delete(allEvents);

        theatreArtBgJob.run();

        return 0;
    }
}
