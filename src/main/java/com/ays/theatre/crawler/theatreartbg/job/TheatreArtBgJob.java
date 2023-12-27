
package com.ays.theatre.crawler.theatreartbg.job;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.utils.PageUtils;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgJob {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    @Inject
    TheatreArtBgDayService service;

    public void run() {
        LOG.info("Starting Theatre.art.bg job");
        try (Playwright playwright = Playwright.create()) {
            try (var browser = playwright.webkit().launch()) {
                try (var page = browser.newPage()) {
                    PageUtils.navigateWithRetry(page, TheatreArtBgConstants.BASE_URL);

                    var calendar = service.getCalendar(page);
                    calendar.forEach(cal -> {
                        try (Page currentPage = browser.newPage()) {
                            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                                                   cal.getMonth(), cal.getYear(), cal.getUrl()));

                            PageUtils.navigateWithRetry(currentPage, cal.getUrl());
                            var nextDays = service.getAllDaysOfMonthsUrls(currentPage);
                            var allDaysLinks = new ArrayList<>(nextDays);
                            allDaysLinks.add(cal.getUrl());

                            var payloads = allDaysLinks.stream()
                                    .map(url -> getPayload(cal, url)).toList();
                            queue.addAll(payloads);
                        }
                    });
                }
            }
        }
    }

    private ImmutableTheatreArtQueuePayload getPayload(ImmutableTheatreArtBgCalendar cal, String url) {
        return ImmutableTheatreArtQueuePayload.builder().url(url).object(cal).build();
    }
}
