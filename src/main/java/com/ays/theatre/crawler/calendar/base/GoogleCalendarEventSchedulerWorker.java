package com.ays.theatre.crawler.calendar.base;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.service.Worker;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorker;
import com.google.api.services.calendar.model.Event;

public class GoogleCalendarEventSchedulerWorker extends Worker<ImmutableGoogleCalendarEventSchedulerPayload> {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarDao dao;

    public GoogleCalendarEventSchedulerWorker(
            ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> queue,
            AtomicInteger workerIdPool,
            GoogleCalendarService googleCalendarService,
            GoogleCalendarDao dao
    ) {
        super(queue, LOG, workerIdPool.getAndIncrement());
        this.googleCalendarService = googleCalendarService;
        this.dao = dao;
    }

    @Override
    public void handlePayload(ImmutableGoogleCalendarEventSchedulerPayload payload) {
        Event event = googleCalendarService.createCalendarEvent(payload);
        LOG.info(String.format("[%d] Created event %s for %s at %s", getId(), event.getId(), payload.getUrl(),
                               payload.getStartTime().toString()));

        dao.upsertEvent(payload.getUrl(), payload.getStartTime(), event.getId());
    }
}
