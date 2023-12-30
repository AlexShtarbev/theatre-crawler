
package com.ays.theatre.crawler.theatreartbg.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ays.theatre.crawler.utils.DateUtils;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import static com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService.HREF;
import static com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService.SCOPE_A;

@Singleton
public class TheatreArtBgJob {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    @Inject
    TheatreArtBgDayService service;

    public void run() {
        LOG.info("Starting Theatre.art.bg job");
        LOG.info("Navigating to " + TheatreArtBgConstants.BASE_URL);
        var doc = PageUtils.navigateWithRetry(TheatreArtBgConstants.BASE_URL);
        try {
            var calendar = service.getCalendar(doc);
            calendar.forEach(cal -> {
                LOG.info(String.format("---Loading all other days for %d %d: %s---",
                        cal.getMonth(), cal.getYear(), cal.getUrl()));

                var monthPage = PageUtils.navigateWithRetry(cal.getUrl());
                var nextDays = service.getAllDaysOfMonthsUrls(monthPage);
                var allDaysLinks = new ArrayList<>(nextDays);
                allDaysLinks.add(cal.getUrl());

                var payloads = allDaysLinks.stream()
                        .map(url -> getPayload(cal, url)).toList();
                queue.addAll(payloads);
            });
        } catch (Exception ex) {
            LOG.error("Failed to get calendar", ex);
        }
    }

    private ImmutableTheatreArtQueuePayload getPayload(ImmutableTheatreArtBgCalendar cal, String url) {
        return ImmutableTheatreArtQueuePayload.builder().url(url).object(cal).build();
    }
}
