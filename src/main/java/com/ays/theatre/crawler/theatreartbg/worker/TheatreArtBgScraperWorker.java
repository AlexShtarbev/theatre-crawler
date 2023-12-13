/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.worker;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

// https://playwright.dev/java/docs/multithreading
public class TheatreArtBgScraperWorker implements Runnable {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);

    private final TheatreArtBgScraperService theatreArtBgScraperService;
    private final Browser browser;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    public TheatreArtBgScraperWorker(TheatreArtBgScraperService theatreArtBgScraperService,
                                     ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue) {
        this.theatreArtBgScraperService = theatreArtBgScraperService;
        this.browser = Playwright.create().webkit().launch();
        this.queue = queue;
    }

    @Override
    public void run() {
        LOG.info("Starting worker");
        while(true) {
            ImmutableTheatreArtQueuePayload payload;
            do {
                payload = queue.poll();
            } while (payload == null);

            LOG.info(String.format("==> Visiting URL: %s", payload.getUrl()));
            try (var page = browser.newPage()) {
                page.setDefaultTimeout(1200000);
                page.navigate(payload.getUrl());
                theatreArtBgScraperService.extractPlayData(page);
                LOG.info(String.format("Finished scraping: %s", payload.getUrl()));
            } catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }
}
