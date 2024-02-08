package com.ays.theatre.crawler.job;

import com.ays.theatre.crawler.calendar.resync.GoogleCalendarReSyncService;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PeriodicRunner {

    private final TheatreArtBgRunner theatreArtBgRunner;
    private final GoogleCalendarReSyncService googleCalendarReSyncService;

    public PeriodicRunner(TheatreArtBgRunner theatreArtBgRunner,
                          GoogleCalendarReSyncService googleCalendarReSyncService) {
        this.theatreArtBgRunner = theatreArtBgRunner;
        this.googleCalendarReSyncService = googleCalendarReSyncService;
    }

//    @Scheduled(cron="0 */5 * * * ?")
    void runTheaterArtBgJob() {
        // Resync in case the DB data was lost or the process is running on a new machine.
        googleCalendarReSyncService.reSync();

        // Run the scraper
        theatreArtBgRunner.run();
    }
}
