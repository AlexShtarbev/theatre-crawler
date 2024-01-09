package com.ays.theatre.crawler.calendar.base;

import static com.ays.theatre.crawler.Configuration.GOOGLE_CALENDAR_WORKER_QUEUE_SIZE;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.WorkerPool;

import jakarta.inject.Named;
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
            LatchService latchService,
            @Named(GOOGLE_CALENDAR_WORKER_QUEUE_SIZE) int poolSize) {
        super(poolSize);
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
