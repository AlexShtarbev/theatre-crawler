package com.ays.theatre.crawler;

import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgJob;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain(name="TheatreCrawlerApplication")
public class TheatreCrawlerApplication implements QuarkusApplication {

    @Inject
    TheatreArtBgJob theatreArtBgJob;

    @Override
    public int run(String... args) throws Exception {
        theatreArtBgJob.run();
        return 0;
    }
}
