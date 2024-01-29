package com.ays.theatre.crawler;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.calendar.resync.GoogleCalendarReSyncService;
import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    @Inject
    TheatreArtBgJob theatreArtBgJob;

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
