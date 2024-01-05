package com.ays.theatre.crawler.theatreartbg.service;

import static com.ays.theatre.crawler.core.utils.DateUtils.BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP;

import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.TheatreService;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgTicketPayload;
import com.ays.theatre.crawler.core.utils.PageUtils;
import jakarta.inject.Singleton;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

            var record = getPlayRecord(url, ratingAndVotes, crewHtml, descriptionHtml);
            LOG.info(String.format("[%s] Storing result", url));
            theatrePlayDao.upsertDetails(record);

            // update the links to any offered tickets
            var ticketsLinks = getTicketsLinks(playInfo, url);
            ticketsLinks.forEach(
                    payload -> theatrePlayDao.updatePlayTicketLink(payload, Constants.THEATRE_ART_BG_ORIGIN));
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

    private List<ImmutableTheatreArtBgTicketPayload> getTicketsLinks(DomNode playInfo, String url) {
        var dates = theatrePlayDao.getDatesOfPlaysByOriginAndUrl(Constants.THEATRE_ART_BG_ORIGIN, url);
        var dayAndMonthYToDateMap = dates.stream()
                .collect(Collectors.toMap(
                        date -> String.format("%d-%d", date.getDayOfMonth(), date.getMonthValue()),
                        Function.identity()));

        var playTicketsDomNodeList = playInfo.querySelectorAll("td.ptd_predi");
        return playTicketsDomNodeList.stream().map(ticketRow -> {
            var ticketLink = ticketRow.querySelector("a.times\\ link_kupi_bilet");
            if (ticketLink == null) {
                return null;
            }
            var dayAndMonth = ticketRow.querySelector("div.mesec_data").getChildNodes();
            var day = dayAndMonth.get(0).getTextContent();
            var month = dayAndMonth.get(1).getTextContent();
            var monthNumber = BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP.get(month.toLowerCase());
            var dateTime = dayAndMonthYToDateMap.get(String.format("%s-%d", day, monthNumber));
            if (dateTime == null) {
                LOG.error(String.format("Failed to locate ticket url for %s and date %s %s", url, day, month));
                return null;
            }
            var ticketUrl = ticketLink.getAttributes().getNamedItem("href").getTextContent();
            return ImmutableTheatreArtBgTicketPayload.builder()
                    .url(url)
                    .date(dateTime)
                    .ticketUrl(String.format("%s%s", Constants.THEATRE_ART_BG_BASE_URL, ticketUrl))
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void configureWebClient(WebClient webClient) {
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(false);
    }

    private static TheatrePlayDetailsRecord getPlayRecord(String url, String ratingAndVotes, String crewHtml,
                                                          String descriptionHtml) {
        return new TheatrePlayDetailsRecord()
                .setUrl(url)
                .setRating(ratingAndVotes)
                .setCrew(crewHtml)
                .setDescription(descriptionHtml)
                .setOrigin(Constants.THEATRE_ART_BG_ORIGIN);
    }
}
