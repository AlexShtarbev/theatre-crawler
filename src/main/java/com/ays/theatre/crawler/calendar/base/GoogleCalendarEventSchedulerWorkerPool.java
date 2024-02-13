package com.ays.theatre.crawler.calendar.base;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.WorkerPool;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarEventSchedulerWorkerPool extends WorkerPool<GoogleCalendarEventSchedulerWorker> {
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarDao dao;
    private final ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> queue;
    private final LatchService latchService;
    private final AtomicInteger workerIdPool;

    public GoogleCalendarEventSchedulerWorkerPool(
            GoogleCalendarService googleCalendarService,
            GoogleCalendarDao dao,
            ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> queue,
            LatchService latchService) {
        this.googleCalendarService = googleCalendarService;
        this.dao = dao;
        this.queue = queue;
        this.latchService = latchService;
        this.workerIdPool = new AtomicInteger(1);
    }
    @Override
    protected GoogleCalendarEventSchedulerWorker getWorker() {
        return new GoogleCalendarEventSchedulerWorker(queue, workerIdPool, googleCalendarService, dao, latchService);
    }
}
