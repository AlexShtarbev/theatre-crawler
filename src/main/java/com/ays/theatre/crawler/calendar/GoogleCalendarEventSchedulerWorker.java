package com.ays.theatre.crawler.calendar;

import static com.ays.theatre.crawler.Configuration.GOOGLE_CALENDAR_EVENT_SCHEDULER_EXECUTOR;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarEventSchedulerWorker implements Runnable {

    private static final int CONCURRENT_SCHEDULERS = 10;

    @Inject
    GoogleCalendarService googleCalendarService;

    @Inject
    ConcurrentLinkedQueue<ImmutableGoogleCalendarEventSchedulerPayload> queue;

    @Inject
    @Named(GOOGLE_CALENDAR_EVENT_SCHEDULER_EXECUTOR)
    Executor executor;

    @Override
    public void run() {
        while (true) {
            var schedulers = new ArrayList<>();
            while (!queue.isEmpty()) {
                for (int i = 0; i < CONCURRENT_SCHEDULERS; i++) {
                    var payload = queue.poll();
                    schedulers.add(CompletableFuture.runAsync(() -> {
                        googleCalendarService.createCalendarEvent(payload.getTitle(), payload.getTheatre(),
                                                                  payload.getUrl(), payload.getStartTime());
                    }, executor));
                }

                try {
                    CompletableFuture.allOf(schedulers.toArray(new CompletableFuture[]{})).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                } finally {
                    schedulers.clear();
                }
            }
        }
    }
}
