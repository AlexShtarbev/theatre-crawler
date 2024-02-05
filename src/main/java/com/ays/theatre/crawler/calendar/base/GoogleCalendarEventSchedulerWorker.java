package com.ays.theatre.crawler.calendar.base;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.Worker;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorker;
import com.google.api.services.calendar.model.Event;

public class GoogleCalendarEventSchedulerWorker extends Worker<ImmutableGoogleCalendarEventSchedulerPayload> {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarDao dao;
    private final LatchService latchService;

    public GoogleCalendarEventSchedulerWorker(
            ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> queue,
            AtomicInteger workerIdPool,
            GoogleCalendarService googleCalendarService,
            GoogleCalendarDao dao,
            LatchService latchService
    ) {
        super(queue, LOG, workerIdPool.getAndIncrement());
        this.googleCalendarService = googleCalendarService;
        this.dao = dao;
        this.latchService = latchService;
    }

    @Override
    public void handlePayload(ImmutableGoogleCalendarEventSchedulerPayload payload) {
        Event event = googleCalendarService.createCalendarEvent(payload);
        LOG.info(String.format("[%d] Created event %s for %s at %s", getId(), event.getId(), payload.getUrl(),
                               payload.getStartTime().toString()));

        try {
            dao.upsertEvent(payload.getUrl(), payload.getStartTime(), event.getId());
        } catch (Exception ex) {
            LOG.error(String.format("Failed to upsert event for %s at %s", payload.getUrl(), payload.getStartTime()));
        }
        latchService.countDown(Constants.GOOGLE_CALENDAR_LATCH);
    }
}
