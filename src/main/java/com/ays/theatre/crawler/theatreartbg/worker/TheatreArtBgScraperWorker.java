
package com.ays.theatre.crawler.theatreartbg.worker;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.TheatreService;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgPlayService;
import com.ays.theatre.crawler.utils.PageUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

// https://playwright.dev/java/docs/multithreading
public class TheatreArtBgScraperWorker implements Runnable {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);

    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final TheatreArtBgDayService theatreArtBgDayService;
    private final TheatreArtBgPlayService theatreArtBgPlayService;
    private final Browser browser;
    private final int id;

    public TheatreArtBgScraperWorker(ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                     TheatreArtBgDayService theatreArtBgDayService,
                                     TheatreArtBgPlayService theatreArtBgPlayService,
                                     AtomicInteger workerIdPool) {
        this.theatreArtBgDayService = theatreArtBgDayService;
        this.theatreArtBgPlayService = theatreArtBgPlayService;
        this.browser = Playwright.create().webkit().launch();
        this.queue = queue;
        this.id = workerIdPool.getAndIncrement();
    }

    @Override
    public void run() {
        LOG.info(String.format("[%d] Starting worker", id));
        while(true) {
            ImmutableTheatreArtQueuePayload payload;
            do {
                payload = queue.poll();
            } while (payload == null);

            LOG.info(String.format("[%d] ==> Visiting URL: %s", id, payload.getUrl()));

            try (var page = browser.newPage()) {
                PageUtils.navigateWithRetry(page, payload.getUrl());
                if (payload.getObject() instanceof ImmutableTheatreArtBgCalendar) { // FIXME - custom object
                    scrapeAndTime(theatreArtBgDayService, (ImmutableTheatreArtBgCalendar) payload.getObject(), page);
                } else if (payload.getObject() instanceof ImmutableTheatreArtBgPlayObject) {
                    scrapeAndTime(theatreArtBgPlayService, (ImmutableTheatreArtBgPlayObject) payload.getObject(), page);
                }

            } catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }

    private <T> void scrapeAndTime(TheatreService<T> service, T obj, Page page) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        try {
            service.scrape(obj, page);
        } finally {
            stopWatch.stop();
            LOG.info(String.format("[%d] Scraping %s took %dms", id, page.url(), stopWatch.getTime()));
        }
    }
}
