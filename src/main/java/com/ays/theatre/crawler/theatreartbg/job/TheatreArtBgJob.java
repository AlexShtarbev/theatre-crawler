
package com.ays.theatre.crawler.theatreartbg.job;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;
import org.jsoup.nodes.Document;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarEventSchedulerWorkerPool;
import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.core.utils.PageUtils;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;

import io.quarkus.logging.Log;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgJob {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);

    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> scraperQueue;
    private final  ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> calendarQueue;
    private final TheatreArtBgDayService service;
    private final LatchService latchService;
    private final  TheatrePlayDao theatrePlayDao;
    private final GoogleCalendarDao googleCalendarDao;
    private final TheatreArtBgScraperWorkerPool theatreArtBgScraperWorkerPool;
    private final GoogleCalendarEventSchedulerWorkerPool googleCalendarEventSchedulerWorkerPool;

    public TheatreArtBgJob(ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> scraperQueue,
                           ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> calendarQueue,
                           TheatreArtBgDayService service,
                           LatchService latchService,
                           TheatrePlayDao theatrePlayDao,
                           GoogleCalendarDao googleCalendarDao,
                           TheatreArtBgScraperWorkerPool theatreArtBgScraperWorkerPool,
                           GoogleCalendarEventSchedulerWorkerPool googleCalendarEventSchedulerWorkerPool) {
        this.scraperQueue = scraperQueue;
        this.calendarQueue = calendarQueue;
        this.service = service;
        this.latchService = latchService;
        this.theatrePlayDao = theatrePlayDao;
        this.googleCalendarDao = googleCalendarDao;
        this.theatreArtBgScraperWorkerPool = theatreArtBgScraperWorkerPool;
        this.googleCalendarEventSchedulerWorkerPool = googleCalendarEventSchedulerWorkerPool;
    }

    public void run() {
        LOG.info("Starting Theatre.art.bg job");
        LOG.info("Navigating to " + Constants.THEATRE_ART_BG_BASE_URL);
        var doc = PageUtils.navigateWithRetry(Constants.THEATRE_ART_BG_BASE_URL);
        var today = OffsetDateTime.now();
        try {
            runScraping(doc);

            runCreatingGoogleCalendarEvents(today);
        } catch (Exception ex) {
            LOG.error("Failed to get calendar", ex);
        }
    }

    private void runScraping(Document doc) {
        theatreArtBgScraperWorkerPool.startWorkers();
        handleScrapingPlays(doc);
        Log.info("Done with scraping for day urls");

        handleScrapingPlayDetails();
        Log.info("Done scraping play urls");
        theatreArtBgScraperWorkerPool.stopWorkers();
    }

    private void handleScrapingPlays(Document doc) {
        var calendar = service.getCalendar(doc);
        var dayUrls = getDayUrls(calendar);
        latchService.init(Constants.THEATRE_ART_BG_DAY_LATCH, dayUrls.size());
        scraperQueue.addAll(dayUrls);
        latchService.await(Constants.THEATRE_ART_BG_DAY_LATCH);
    }

    private List<ImmutableTheatreArtQueuePayload> getDayUrls(List<ImmutableTheatreArtBgCalendar> calendar) {
        return calendar.stream().flatMap(cal -> {
            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                                   cal.getMonth(), cal.getYear(), cal.getUrl()));

            var monthPage = PageUtils.navigateWithRetry(cal.getUrl());
            var nextDays = service.getAllDaysOfMonthsUrls(monthPage);
            var allDaysLinks = new ArrayList<>(nextDays);
            allDaysLinks.add(cal.getUrl());

            return allDaysLinks.stream().map(url -> getPayload(cal, url));
        }).toList();
    }

    private void handleScrapingPlayDetails() {
        var allPlayRecords = theatrePlayDao.getTheatrePlaysByOriginAndDatePaged(Constants.THEATRE_ART_BG_ORIGIN,
                OffsetDateTime.now());

        var playPayloads = allPlayRecords.stream().map(url ->
            ImmutableTheatreArtQueuePayload.builder().url(url)
                    .object(ImmutableTheatreArtBgPlayObject.builder().build())
                    .build()
        ).toList();
        latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, playPayloads.size());
        scraperQueue.addAll(playPayloads);
        latchService.await(Constants.THEATRE_ART_BG_PLAY_LATCH);
    }

    private void runCreatingGoogleCalendarEvents(OffsetDateTime today) {
        googleCalendarEventSchedulerWorkerPool.startWorkers();
        var recordsFromTodayOnwards = googleCalendarDao.getRecords(Constants.THEATRE_ART_BG_ORIGIN, today);
        latchService.init(Constants.GOOGLE_CALENDAR_LATCH, recordsFromTodayOnwards.size());
        calendarQueue.addAll(recordsFromTodayOnwards);
        latchService.await(Constants.GOOGLE_CALENDAR_LATCH);
        googleCalendarEventSchedulerWorkerPool.stopWorkers();
    }

    private ImmutableTheatreArtQueuePayload getPayload(ImmutableTheatreArtBgCalendar cal, String url) {
        return ImmutableTheatreArtQueuePayload.builder().url(url).object(cal).build();
    }
}
