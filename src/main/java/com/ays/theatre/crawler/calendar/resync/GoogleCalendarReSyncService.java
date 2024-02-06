package com.ays.theatre.crawler.calendar.resync;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.ays.theatre.crawler.calendar.base.GoogleCalendarService;
import com.ays.theatre.crawler.calendar.dao.GoogleCalendarDao;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.model.ImmutableResycRecord;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.core.utils.DateUtils;
import com.ays.theatre.crawler.core.utils.Origin;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.google.api.services.calendar.model.Event;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarReSyncService {
    private static final Logger LOG = Logger.getLogger(GoogleCalendarReSyncService.class);

    public static final String PLAY_LINK = "play_link";
    public static final String CREW = "crew";
    public static final String RATING = "rating";
    public static final String DESCRIPTION = "description";
    public static final String LAST_SYNC_TIME = "last_sync_time";
    public static final String THEATRE_ART_BG_TICKET_URL = "theatre_art_ticket";

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarDao dao;

    public GoogleCalendarReSyncService(GoogleCalendarService googleCalendarService, GoogleCalendarDao dao) {
        this.googleCalendarService = googleCalendarService;
        this.dao = dao;
    }

    public void reSync() {
        var numberOfEvents = dao.getNumberOfEvents();
        if (numberOfEvents > 0) {
            return;
        }
        var allEvents = googleCalendarService.getAllEvents(OffsetDateTime.now());
        var records = allEvents.stream().map(this::getRecordPair).toList();
        dao.insertRecords(records);
    }

    public ImmutableResycRecord getRecordPair(Event event) {
        var eventPayload = eventToTheatreRecords(event);
        var origin = getOriginFromUrl(eventPayload.getUrl());
        var playRecord = new TheatrePlayRecord()
                .setUrl(eventPayload.getUrl())
                .setTheatre(eventPayload.getTheatre())
                .setTitle(eventPayload.getTitle())
                .setOrigin(origin)
                .setDate(eventPayload.getStartTime())
                .setLastUpdated(OffsetDateTime.now())
                .setTicketsUrl(eventPayload.getTheatreArtBgTicket().orElse(null));

        var detailsRecord = new TheatrePlayDetailsRecord()
                .setUrl(eventPayload.getUrl())
                .setDescription(eventPayload.getDescription())
                .setCrew(eventPayload.getCrew())
                .setRating(eventPayload.getRating())
                .setOrigin(origin);

        return ImmutableResycRecord.builder()
                .playRecord(playRecord)
                .detailsRecord(detailsRecord)
                .build();
    }

    public ImmutableGoogleCalendarEventSchedulerPayload eventToTheatreRecords(Event event) {
        var doc = Jsoup.parse(event.getDescription());
        var lastSyncTime = getMaybeElement(doc, LAST_SYNC_TIME).map(Element::text).map(OffsetDateTime::parse);
        if (lastSyncTime.isEmpty()) {
            LOG.warn(String.format("No last sync time for event %s: %s @ %s", event.getId(), event.getSummary(),
                                   event.getStart().toString()));
        }
        return ImmutableGoogleCalendarEventSchedulerPayload.builder()
                .title(event.getSummary())
                .theatre(event.getLocation())
                .startTime(DateUtils.toOffsetDateTime(event.getStart()))
                .url(getPlayLink(doc))
                .crew(getElement(doc, CREW).html())
                .rating(getElement(doc, RATING).text())
                .lastUpdated(lastSyncTime.orElseGet(OffsetDateTime::now))
                .description(getElement(doc, DESCRIPTION).html())
                .theatreArtBgTicket(getMaybeElement(doc, THEATRE_ART_BG_TICKET_URL).map(Element::html))
                .build();
    }

    private String getPlayLink(Document doc) {
        return getElement(doc, PLAY_LINK).attributes().get("href");
    }

    private String getOriginFromUrl(String url) {
        if (url.startsWith(Constants.THEATRE_ART_BG_BASE_URL)) {
            return Origin.THEATRE_ART_BG.getOrigin();
        }
        throw new RuntimeException("Unknown origin for " + url);
    }

    private Element getElement(Document doc, String elementClass) {
        var elements = getMaybeElement(doc, elementClass);
        if (elements.isEmpty()) {
            throw new RuntimeException(String.format("No element with class %s found. Exactly one expected",
                                                     elementClass));
        }

        return elements.get();
    }

    private Optional<Element> getMaybeElement(Document doc, String elementClass) {
        var elements = doc.getElementsByClass(elementClass);
        if (elements.isEmpty()) {
            return Optional.empty();
        }
        if (elements.size() > 1) {
            throw new RuntimeException(String.format("More than one %s class found. Exactly one expected",
                                                     elementClass));
        }

        return Optional.of(elements.getFirst());
    }
}
