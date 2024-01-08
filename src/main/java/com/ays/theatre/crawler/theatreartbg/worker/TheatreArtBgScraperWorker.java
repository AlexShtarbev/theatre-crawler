
package com.ays.theatre.crawler.theatreartbg.worker;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;

import com.ays.theatre.crawler.core.service.TheatreService;
import com.ays.theatre.crawler.core.service.Worker;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;

// https://playwright.dev/java/docs/multithreading
public class TheatreArtBgScraperWorker extends Worker<ImmutableTheatreArtQueuePayload> {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);
    private final TheatreArtBgDayService theatreArtBgDayService;
    private final TheatreArtBgPlayService theatreArtBgPlayService;

    public TheatreArtBgScraperWorker(ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                     TheatreArtBgDayService theatreArtBgDayService,
                                     TheatreArtBgPlayService theatreArtBgPlayService,
                                     AtomicInteger workerIdPool) {
        super(queue, LOG, workerIdPool.getAndIncrement());
        this.theatreArtBgDayService = theatreArtBgDayService;
        this.theatreArtBgPlayService = theatreArtBgPlayService;
    }

    @Override
    public void handlePayload(ImmutableTheatreArtQueuePayload payload) {
        try {
            if (payload.getObject() instanceof ImmutableTheatreArtBgCalendar) { // FIXME - custom object
                LOG.info(String.format("[%d] ==> Visiting Day URL: %s", getId(), payload.getUrl()));
                scrapeAndTime(theatreArtBgDayService, (ImmutableTheatreArtBgCalendar) payload.getObject(),
                        payload.getUrl());
            } else if (payload.getObject() instanceof ImmutableTheatreArtBgPlayObject) {
                LOG.info(String.format("[%d] ==> Visiting PLAY URL: %s", getId(), payload.getUrl()));
                scrapeAndTime(theatreArtBgPlayService, (ImmutableTheatreArtBgPlayObject) payload.getObject(),
                        payload.getUrl());
            } else {
                LOG.error("No matching service for " + payload.getUrl());
            }

        } catch (Exception ex) {
            LOG.error(ex);
        }
    }

    private <T> void scrapeAndTime(TheatreService<T> service, T obj, String url) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        try {
            service.scrape(obj, url);
        } catch (Exception ex) {
            LOG.error(ex);
        } finally {
            stopWatch.stop();
            LOG.info(String.format("[%d] Scraping %s took %dms", getId(), url, stopWatch.getTime()));
        }
    }
}
