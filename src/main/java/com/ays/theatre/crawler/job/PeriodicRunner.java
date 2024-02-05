package com.ays.theatre.crawler.job;

import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PeriodicRunner {

    private final TheatreArtBgRunner theatreArtBgRunner;

    public PeriodicRunner(TheatreArtBgRunner theatreArtBgRunner) {
        this.theatreArtBgRunner = theatreArtBgRunner;
    }

    @Scheduled(cron="0 */5 * * * ?")
    void runTheaterArtBgJob() {
        theatreArtBgRunner.run();
    }
}
