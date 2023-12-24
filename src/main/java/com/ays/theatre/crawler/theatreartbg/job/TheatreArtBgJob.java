
package com.ays.theatre.crawler.theatreartbg.job;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgCrawlerService;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgJob {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);
    public static final int MAX_NUM_RETRIES = 10;

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    @Inject
    TheatreArtBgCrawlerService theatreArtBgCrawlerService;

    public void run() {
        try (Playwright playwright = Playwright.create()) {
            try (var browser = playwright.webkit().launch()) {
                try (var page = browser.newPage()) {
                    page.navigate(TheatreArtBgConstants.BASE_URL);

                    var calendar = theatreArtBgCrawlerService.getCalendar(page);
                    calendar.forEach(cal -> {
                        try (Page currentPage = browser.newPage()) {
                            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                                                   cal.getMonth(), cal.getYear(), cal.getUrl()));

                            navigate(currentPage, cal.getUrl());
                            var nextDays = theatreArtBgCrawlerService.getAllDaysOfMonthsUrls(currentPage);
                            var allDaysLinks = new ArrayList<>(nextDays);
                            allDaysLinks.add(cal.getUrl());

                            var payloads = allDaysLinks.stream()
                                    .map(url -> getCalendar(cal, url)).toList();
                            queue.addAll(payloads);
                        }
                    });
                }
            }
        }
    }

    private void navigate(Page currentPage, String url) {
        for (int i = 0; i < MAX_NUM_RETRIES; i++) {
            try {
                currentPage.setDefaultTimeout(30_000);
                currentPage.navigate(url);
            } catch (Exception ex) {
                LOG.info("Will retry failed navigation");
                LOG.error(ex);
            }
        }
    }

    private ImmutableTheatreArtQueuePayload getCalendar(ImmutableTheatreArtBgCalendar cal, String url) {
        return ImmutableTheatreArtQueuePayload.builder().url(url).calendar(cal).build();
    }
}
