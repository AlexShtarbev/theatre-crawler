/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.service;

import java.time.OffsetDateTime;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.TheatreService;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.microsoft.playwright.Page;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgPlayService implements TheatreService<ImmutableTheatreArtBgPlayObject> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgPlayService.class);

    @Inject
    TheatrePlayDao theatrePlayDao;

    @Override
    public void scrape(ImmutableTheatreArtBgPlayObject obj, Page page) {
        var playInfo = page.locator("div.actior");
        var descriptionHtml =  playInfo.locator(":scope>div:below(table)>>nth=0").innerHTML();

        var productionCrew = playInfo.locator(":scope>table>tbody>tr>td>>nth=1");
        var crewHtml = productionCrew.innerHTML().split("<table")[0];

        var result = crewHtml + "<br>" + descriptionHtml;
        result = result.replaceAll("\n", "").replace("\t", "");
        LOG.info(String.format("%s", result));
        theatrePlayDao.upsertDetails(page.url(), result);
    }
}
