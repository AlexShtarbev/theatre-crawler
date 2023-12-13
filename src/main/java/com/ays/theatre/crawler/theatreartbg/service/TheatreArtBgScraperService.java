package com.ays.theatre.crawler.theatreartbg.service;

import org.jboss.logging.Logger;

import com.microsoft.playwright.Page;

import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgScraperService {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperService.class);

    public void extractPlayData(Page page) {
        page.waitForSelector("td.left");
        var tableWithPlays = page.locator("td.left").locator("#left");

        var rows = tableWithPlays.locator(":scope div.postanovka tr");
        var rowCount = rows.count();
        LOG.info("logs " + rowCount);

        if (rowCount == 0) {
            return;
        }

        var links = rows.locator(":scope div.text h2 a");
        var linkCounts = links.count();
        LOG.info("links: " + linkCounts);


        for (int index = 0; index < linkCounts; index++) {
            var  link = links.nth(index);
            var href = link.getAttribute("href");
            var title = link.getAttribute("title");
            LOG.info(String.format("%s: %s", title, href));
        }
    }
}
