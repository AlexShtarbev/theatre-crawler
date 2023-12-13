/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.job;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgCrawlerService;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;
import com.ays.theatre.crawler.theatreartbg.worker.TheatreArtBgScraperWorkerPool;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgJob {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);
    private static final int PARALLEL_WORKERS_SIZE = 5;

    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue = new ConcurrentLinkedQueue<>();

    @Inject
    TheatreArtBgCrawlerService theatreArtBgCrawlerService;

    @Inject
    TheatreArtBgScraperService theatreArtBgScraperService;

    public void run() {
        var workerPool = new TheatreArtBgScraperWorkerPool(theatreArtBgScraperService, queue, PARALLEL_WORKERS_SIZE);
        workerPool.startWorkers();
        try (Playwright playwright = Playwright.create()) {
            try (var browser = playwright.webkit().launch()) {
                try (var page = browser.newPage()) {
                    page.navigate(TheatreArtBgConstants.BASE_URL);

                    var calendar = theatreArtBgCrawlerService.getCalendar(page);
                    calendar.forEach(cal -> {
                        try (Page currentPage = browser.newPage()) {
                            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                                                   cal.getMonth(), cal.getYear(), cal.getUrl()));

                            currentPage.navigate(cal.getUrl());
                            var nextDays = theatreArtBgCrawlerService.getAllDaysOfMonthsUrls(currentPage);
                            var allDaysLinks = new ArrayList<>(nextDays);
                            allDaysLinks.add(cal.getUrl());

                            var payloads = allDaysLinks.stream()
                                    .map(url -> ImmutableTheatreArtQueuePayload.builder().url(url).build()).toList();
                            queue.addAll(payloads);
                        }
                    });
                }
            }
        }
    }
}
