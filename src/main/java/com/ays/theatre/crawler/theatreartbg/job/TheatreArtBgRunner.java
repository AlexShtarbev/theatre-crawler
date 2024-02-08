
package com.ays.theatre.crawler.theatreartbg.job;

import static com.ays.theatre.crawler.Configuration.GOOGLE_CALENDAR_WORKER_QUEUE_SIZE;
import static com.ays.theatre.crawler.Configuration.THEATRE_ART_BG_WORKER_POOL_SIZE;

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
import com.ays.theatre.crawler.core.utils.Origin;
import com.ays.theatre.crawler.core.utils.PageUtils;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;

import io.quarkus.logging.Log;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgRunner implements Runnable {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgRunner.class);

    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> scraperQueue;
    private final  ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> calendarQueue;
    private final TheatreArtBgDayService service;
    private final LatchService latchService;
    private final  TheatrePlayDao theatrePlayDao;
    private final GoogleCalendarDao googleCalendarDao;
    private final TheatreArtBgScraperWorkerPool theatreArtBgScraperWorkerPool;
    private final GoogleCalendarEventSchedulerWorkerPool googleCalendarEventSchedulerWorkerPool;
    private final int theatreArtBgWorkerPoolSize;
    private final int googleCalendarWorkerPoolSize;

    public TheatreArtBgRunner(ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> scraperQueue,
                              ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> calendarQueue,
                              TheatreArtBgDayService service,
                              LatchService latchService,
                              TheatrePlayDao theatrePlayDao,
                              GoogleCalendarDao googleCalendarDao,
                              TheatreArtBgScraperWorkerPool theatreArtBgScraperWorkerPool,
                              GoogleCalendarEventSchedulerWorkerPool googleCalendarEventSchedulerWorkerPool,
                              @Named(THEATRE_ART_BG_WORKER_POOL_SIZE) int theatreArtBgWorkerPoolSize,
                              @Named(GOOGLE_CALENDAR_WORKER_QUEUE_SIZE) int googleCalendarWorkerPoolSize) {
        this.scraperQueue = scraperQueue;
        this.calendarQueue = calendarQueue;
        this.service = service;
        this.latchService = latchService;
        this.theatrePlayDao = theatrePlayDao;
        this.googleCalendarDao = googleCalendarDao;
        this.theatreArtBgScraperWorkerPool = theatreArtBgScraperWorkerPool;
        this.googleCalendarEventSchedulerWorkerPool = googleCalendarEventSchedulerWorkerPool;
        this.theatreArtBgWorkerPoolSize = theatreArtBgWorkerPoolSize;
        this.googleCalendarWorkerPoolSize = googleCalendarWorkerPoolSize;
    }

    public void run() {
        LOG.info("Starting Theatre.art.bg job");
        LOG.info("Navigating to " + Constants.THEATRE_ART_BG_BASE_URL);
        var doc = PageUtils.navigateWithRetry(Constants.THEATRE_ART_BG_BASE_URL);
        var currentTime = OffsetDateTime.now();
        try {
            runScraping(currentTime, doc);

            runCreatingGoogleCalendarEvents(currentTime);
        } catch (Exception ex) {
            LOG.error("Failed to get calendar", ex);
        } finally {
            latchService.clear();
        }
    }

    private void runScraping(OffsetDateTime currentTime, Document doc) {
        theatreArtBgScraperWorkerPool.startWorkers(theatreArtBgWorkerPoolSize);
        handleScrapingPlays(currentTime, doc);
        Log.info("Done with scraping for day urls");

        handleScrapingPlayDetails(currentTime);
        Log.info("Done scraping play urls");
        theatreArtBgScraperWorkerPool.stopWorkers();
    }

    private void handleScrapingPlays(OffsetDateTime currentTime, Document doc) {
        var calendar = service.getCalendar(doc);
        var dayUrls = getDayUrls(currentTime, calendar);
        latchService.init(Constants.THEATRE_ART_BG_DAY_LATCH, dayUrls.size());
        scraperQueue.addAll(dayUrls);
        latchService.await(Constants.THEATRE_ART_BG_DAY_LATCH);
    }

    private List<ImmutableTheatreArtQueuePayload> getDayUrls(
            OffsetDateTime currentTIme,
            List<ImmutableTheatreArtBgCalendar> calendar) {

        return calendar.stream().flatMap(cal -> {
            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                                   cal.getMonth(), cal.getYear(), cal.getUrl()));

            var monthPage = PageUtils.navigateWithRetry(cal.getUrl());
            var nextDays = service.getAllDaysOfMonthsUrls(monthPage);
            var allDaysLinks = new ArrayList<>(nextDays);
            allDaysLinks.add(cal.getUrl());

            return allDaysLinks.stream().map(url -> getPayload(currentTIme, cal, url));
        }).toList();
    }

    private void handleScrapingPlayDetails(OffsetDateTime currentTime) {
        var allPlayRecords = theatrePlayDao.getTheatrePlaysByOrigin(Origin.THEATRE_ART_BG, currentTime);
        var playPayloads = allPlayRecords.stream().map(url ->
            ImmutableTheatreArtQueuePayload.builder().url(url)
                    .object(ImmutableTheatreArtBgPlayObject.builder().build())
                    .scrapingStartTime(currentTime)
                    .build()
        ).toList();
        latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, playPayloads.size());
        scraperQueue.addAll(playPayloads);
        latchService.await(Constants.THEATRE_ART_BG_PLAY_LATCH);
    }

    private void runCreatingGoogleCalendarEvents(OffsetDateTime today) {
        var recordsFromTodayOnwards = googleCalendarDao.getRecords(Origin.THEATRE_ART_BG, today);
        if (recordsFromTodayOnwards.isEmpty()) {
            LOG.info("No new events to create. Won't start Google Calendar workers.");
            return;
        }
        var poolSize = googleCalendarWorkerPoolSize;
        if (recordsFromTodayOnwards.size() < poolSize) {
            poolSize = recordsFromTodayOnwards.size();
        }

        latchService.init(Constants.GOOGLE_CALENDAR_LATCH, recordsFromTodayOnwards.size());
        googleCalendarEventSchedulerWorkerPool.startWorkers(poolSize);
        calendarQueue.addAll(recordsFromTodayOnwards);
        latchService.await(Constants.GOOGLE_CALENDAR_LATCH);
        googleCalendarEventSchedulerWorkerPool.stopWorkers();
    }

    private ImmutableTheatreArtQueuePayload getPayload(
            OffsetDateTime currentTime,
            ImmutableTheatreArtBgCalendar cal,
            String url) {
        return ImmutableTheatreArtQueuePayload.builder()
                .url(url)
                .object(cal)
                .scrapingStartTime(currentTime)
                .build();
    }
}
