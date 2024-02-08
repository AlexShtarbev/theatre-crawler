package com.ays.theatre.crawler.theatreartbg.service;

import static com.ays.theatre.crawler.Configuration.CUSTOM_DSL;
import static com.ays.theatre.crawler.core.utils.DateUtils.BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP;

import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.ScrapeService;
import com.ays.theatre.crawler.core.utils.DateUtils;
import com.ays.theatre.crawler.core.utils.Origin;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgTicketPayload;
import com.ays.theatre.crawler.core.utils.PageUtils;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TheatreArtBgPlayService implements ScrapeService<ImmutableTheatreArtBgPlayObject> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgPlayService.class);

    private final TheatrePlayDao theatrePlayDao;
    private final LatchService latchService;
    private final DSLContext dslContext;

    public TheatreArtBgPlayService(
            TheatrePlayDao theatrePlayDao,
            LatchService latchService,
            @Named(CUSTOM_DSL) DSLContext dslContext) {
        this.theatrePlayDao = theatrePlayDao;
        this.latchService = latchService;
        this.dslContext = dslContext;
    }

    @Override
    public void scrape(ImmutableTheatreArtBgPlayObject obj, String url, OffsetDateTime scrapeStartTime) {
        HtmlPage page = null;
        try (WebClient webClient = new WebClient()) {
            LOG.info(String.format("[%s] Will try to get a connection", url));
            configureWebClient(webClient);
            page = PageUtils.navigateWithRetry(webClient, url);
            LOG.info(String.format("[%s] Will get play info ", url));
            var playInfo = page.querySelectorAll("div.actior").getFirst();

            var descriptionHtml = getDescriptionHtml(playInfo);
            LOG.info(String.format("[%s] Got play description", url));

            var crewHtml = getCrewHtml(playInfo);
            LOG.info(String.format("[%s] Got play crew", url));

            var ratingAndVotes = getRatingAndVotes(page);
            // update the links to any offered tickets
            var ticketsLinks = getTicketsLinks(playInfo, url);

            dslContext.transaction(tx -> {
                var dslContext = tx.dsl();
                var record = getPlayRecord(scrapeStartTime, url, ratingAndVotes, crewHtml, descriptionHtml);
                LOG.info(String.format("[%s] Storing result", url));
                theatrePlayDao.upsertDetails(dslContext, record);

                ticketsLinks.forEach(
                        payload -> theatrePlayDao.updatePlayTicketLink(dslContext, payload, Origin.THEATRE_ART_BG));
            });
        } catch (Exception ex) {
            LOG.error("An error occurred while trying to scrape " + url);
            throw new RuntimeException(ex);
        } finally {
            if (Objects.nonNull(page)) {
                page.cleanUp();
            }
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

    // TODO: Find a way to calculate the year based on the month
    private List<ImmutableTheatreArtBgTicketPayload> getTicketsLinks(DomNode playInfo, String url) {
        var dates = theatrePlayDao.getDatesOfPlaysByOriginAndUrl(Origin.THEATRE_ART_BG, url);

        var playTicketsDomNodeList = playInfo.querySelectorAll("td.ptd_predi");
        return playTicketsDomNodeList.stream()
                .flatMap(ticketRow -> extractTicketMetadata(url, ticketRow))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Stream<ImmutableTheatreArtBgTicketPayload> extractTicketMetadata(String url, DomNode ticketRow) {
        var ticketLink = ticketRow.querySelector("a.times\\ link_kupi_bilet");
        if (ticketLink == null) {
            return null;
        }
        var ticketUrl = ticketLink.getAttributes().getNamedItem("href").getTextContent();

        var dayAndMonth = ticketRow.querySelector("div.mesec_data").getChildNodes();
        var day = Integer.parseInt(dayAndMonth.get(0).getTextContent());
        var month = dayAndMonth.get(1).getTextContent();
        var monthNumber = BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP.get(month.toLowerCase());
        var allTicketRows = ticketRow.querySelectorAll("table td");
        return getAllTicketsForEachDay(url, allTicketRows, day, monthNumber, ticketUrl);
    }

    private Stream<ImmutableTheatreArtBgTicketPayload> getAllTicketsForEachDay(
            String url,
            DomNodeList<DomNode> allTicketRows,
            int day,
            Integer monthNumber,
            String ticketUrl) {

        return allTicketRows
                .stream()
                .flatMap(t -> t.getChildNodes().stream())
                .filter(o -> o instanceof DomText)
                .map(t -> t.getTextContent().trim())
                .filter(t -> !Objects.equals(t, ""))
                .map(dateTimeText ->
                             getImmutableTheatreArtBgTicketPayload(url, dateTimeText, day, monthNumber, ticketUrl));
    }

    private ImmutableTheatreArtBgTicketPayload getImmutableTheatreArtBgTicketPayload(
            String url,
            String dateTimeText,
            int day,
            Integer month,
            String ticketUrl) {

        var date = OffsetDateTime.ofInstant(
                DateUtils.getDateWithoutTimeUsingCalendar().toInstant(), ZoneId.of(Constants.TIMEZONE));
        var year = date.getYear();
        var hourAndMinute = dateTimeText.split(" ")[0];
        if (!hourAndMinute.contains(".")) {
            return null;
        }
        var hourAndMinuteParts = hourAndMinute.split("\\.");
        var hour = Integer.parseInt(hourAndMinuteParts[0]) + 2; // FIXME
        var minute = Integer.parseInt(hourAndMinuteParts[1]);
        if (date.getMonthValue() > month) {
            year++;
        }
        return ImmutableTheatreArtBgTicketPayload.builder()
                .url(url)
                .date(DateUtils.toOffsetDateTime(year, month, day, hour, minute))
                .ticketUrl(String.format("%s%s", Constants.THEATRE_ART_BG_BASE_URL, ticketUrl))
                .build();
    }

    private void configureWebClient(WebClient webClient) {
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setDownloadImages(false);
    }

    private TheatrePlayDetailsRecord getPlayRecord(
            OffsetDateTime scrapeStartTime,
            String url,
            String ratingAndVotes,
            String crewHtml,
            String descriptionHtml) {
        return new TheatrePlayDetailsRecord()
                .setUrl(url)
                .setRating(ratingAndVotes)
                .setCrew(crewHtml)
                .setDescription(descriptionHtml)
                .setOrigin(Origin.THEATRE_ART_BG.getOrigin())
                .setLastUpdated(scrapeStartTime);
    }
}
