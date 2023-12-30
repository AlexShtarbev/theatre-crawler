/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.service;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.TheatreService;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlPage;
import org.jboss.logging.Logger;

import java.util.stream.Collectors;

@Singleton
public class TheatreArtBgPlayService implements TheatreService<ImmutableTheatreArtBgPlayObject> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgPlayService.class);

    @Inject
    TheatrePlayDao theatrePlayDao;

    @Override
    public void scrape(ImmutableTheatreArtBgPlayObject obj,  String url) {
        try (WebClient webClient = new WebClient()) {
            LOG.info(String.format("[%s] Will try to get a connection to: ", url));
            configureWebClient(webClient);
            HtmlPage page = webClient.getPage(url);
            LOG.info(String.format("[%s] Will get play info ", url));
            var playInfo = page.querySelectorAll("div.actior").getFirst();

            var descriptionHtml = playInfo.getChildNodes().stream()
                    .filter(domNode -> "div".equals(domNode.getLocalName()))
                    .findFirst()
                    .map(DomNode::asXml)
                    .orElse("")
                    .replaceAll("\u0000", ""); // remove null characters as per https://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0;

            LOG.info(String.format("[%s] Got play description", url));

            var productionCrew = playInfo.querySelectorAll("table>tbody>tr>td").get(1);
            var crewHtml = productionCrew.asXml().split("<table")[0].replaceAll("\u0000", ""); // remove null characters as per https://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0;
            LOG.info(String.format("[%s] Got play crew", url));

            var ratingTable = page.querySelectorAll("div.right_content").getFirst().querySelectorAll("table>tbody>tr>td").get(1);
            var ratingAndVotes = ratingTable.querySelectorAll("fount").stream().map(DomNode::asXml).collect(Collectors.joining(" "));

            var record = new TheatrePlayDetailsRecord()
                    .setUrl(url)
                    .setRating(ratingAndVotes)
                    .setCrew(crewHtml)
                    .setDescription(descriptionHtml);
            LOG.info(String.format("[%s] Storing result", url));
            theatrePlayDao.upsertDetails(record);
        } catch (Exception ex) {
            LOG.error("An error occurred while trying to scrape " + url);
            throw new RuntimeException(ex);
        } finally {
            LOG.info(String.format("[%s] Finished scraping", url));
        }
    }

    private void configureWebClient(WebClient webClient) {
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(false);
    }
}
