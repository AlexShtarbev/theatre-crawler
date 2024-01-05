package com.ays.theatre.crawler.calendar.resync;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.utils.DateUtils;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.google.api.services.calendar.model.Event;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarReSyncService {
    public static final String PLAY_LINK = "play_link";
    public static final String CREW = "crew";
    public static final String RATING = "rating";
    public static final String DESCRIPTION = "description";
    public static final String LAST_SYNC_TIME = "last_sync_time";
    public static final String THEATRE_ART_BG_TICKET_URL = "theatre_art_ticket";

    private final TheatrePlayDao dao;

    public GoogleCalendarReSyncService(TheatrePlayDao dao) {
        this.dao = dao;
    }

    public void reSyncEvent(Event event, String origin) {
        var eventPayload = eventToTheatreRecords(event);
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

    }

    public ImmutableGoogleCalendarEventSchedulerPayload eventToTheatreRecords(Event event) {
        var doc = Jsoup.parse(event.getDescription());
        return ImmutableGoogleCalendarEventSchedulerPayload.builder()
                .title(event.getSummary())
                .theatre(event.getLocation())
                .startTime(DateUtils.toOffsetDateTime(event.getStart()))
                .url(getElement(doc, PLAY_LINK).text())
                .crew(getElement(doc, CREW).html())
                .rating(getElement(doc, RATING).text())
                .lastUpdated(OffsetDateTime.parse(getElement(doc, LAST_SYNC_TIME).text()))
                .description(getElement(doc, DESCRIPTION).html())
                .theatreArtBgTicket(getMaybeElement(doc, THEATRE_ART_BG_TICKET_URL).map(Element::html))
                .build();
    }

    private Element getElement(Document doc, String elementClass) {
        var elements = doc.getElementsByClass(elementClass);
        if (elements.isEmpty()) {
            throw new RuntimeException(String.format("No element with class %s found. Exactly one expected",
                                                     elementClass));
        }
        if (elements.size() > 1) {
            throw new RuntimeException(String.format("More than one %s class found. Exactly one expected",
                                                     elementClass));
        }

        return elements.getFirst();
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
