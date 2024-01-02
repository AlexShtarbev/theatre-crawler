package com.ays.theatre.crawler.theatreartbg.service;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.LatchService;
import com.ays.theatre.crawler.global.service.TheatreService;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.global.Constants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.utils.PageUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;
import org.jboss.logging.Logger;

import java.util.Objects;

@Singleton
public class TheatreArtBgPlayService implements TheatreService<ImmutableTheatreArtBgPlayObject> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgPlayService.class);

    private final TheatrePlayDao theatrePlayDao;
    private final LatchService latchService;

    public TheatreArtBgPlayService(TheatrePlayDao theatrePlayDao, LatchService latchService) {
        this.theatrePlayDao = theatrePlayDao;
        this.latchService = latchService;
    }

    @Override
    public void scrape(ImmutableTheatreArtBgPlayObject obj,  String url) {
        try (WebClient webClient = new WebClient()) {
            LOG.info(String.format("[%s] Will try to get a connection to: ", url));
            configureWebClient(webClient);
            HtmlPage page = PageUtils.navigateWithRetry(webClient, url);
            LOG.info(String.format("[%s] Will get play info ", url));
            var playInfo = page.querySelectorAll("div.actior").getFirst();

            var descriptionHtml = getDescriptionHtml(playInfo);
            LOG.info(String.format("[%s] Got play description", url));

            var crewHtml = getCrewHtml(playInfo);
            LOG.info(String.format("[%s] Got play crew", url));

            var ratingAndVotes = getRatingAndVotes(page);

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
            latchService.countDown(Constants.THEATRE_ART_BG_PLAY_LATCH);
            LOG.info(String.format("[%s] Finished scraping", url));
        }
    }

    private String getCrewHtml(DomNode playInfo) {
        var productionCrew = playInfo.querySelectorAll("table>tbody>tr>td").get(1);
        var children = productionCrew.getChildNodes();
        var builder = new StringBuilder();
        boolean hasAddedCrewTypeLabel = false;
        boolean hasAddedFirstCrewName = false;
        for (DomNode node : children) {
            if (Objects.equals(node.getLocalName(), "table")) {
                break;
            } else if (node instanceof HtmlFont) {
                var crewType = node.getTextContent();
                if (!hasAddedCrewTypeLabel) {
                    hasAddedCrewTypeLabel = true;
                    hasAddedFirstCrewName = false;
                } else {
                    builder.append("\n");
                }
                builder.append(crewType);
            } else if (node instanceof DomText) {
                var text = node.getTextContent();
                if (text.isBlank() || text.isEmpty()) {
                    continue;
                }
                builder.append(text);
            } else if (node instanceof HtmlAnchor) {
                var subLink = node.getAttributes().getNamedItem("href");
                var crewName = node.getTextContent();
                if (!hasAddedFirstCrewName) {
                    hasAddedFirstCrewName = true;
                } else {
                    builder.append("\n\t");
                }
                builder.append(String.format("<a href=\"%s%s\">%s</a>", Constants.THEATRE_ART_BG_BASE_URL,
                        subLink.getTextContent(), crewName));
            }
        }
        return builder.toString()
                // https://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0;
                .replaceAll("\u0000", "");
    }

    private String getDescriptionHtml(DomNode playInfo) {
        var divisions = playInfo.querySelectorAll("div.field-item\\ even").stream().toList();
        var htmlDescirption = "";
        if (!divisions.isEmpty()) {
            htmlDescirption = divisions.getLast().asXml();
        } else {
            divisions = playInfo.getChildNodes().stream().filter(c -> c instanceof HtmlDivision).toList();
            if (!divisions.isEmpty()) {
                htmlDescirption = divisions.getFirst().asXml();
            }
        }
        return htmlDescirption
                // https://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0;
                .replaceAll("\u0000", "");
    }

    private String getRatingAndVotes(HtmlPage page) {
        return page.querySelectorAll("div.right_content").get(1)
                .querySelectorAll("table>tbody>tr>td").get(1).getTextContent()
                .replaceAll(" ", "")
                .replace("\n", " ")
                // https://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0;
                .replaceAll("\u0000", "");
    }

    private void configureWebClient(WebClient webClient) {
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(false);
    }
}
