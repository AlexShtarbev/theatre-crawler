package com.ays.theatre.crawler.calendar.base;

import java.time.OffsetDateTime;

import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarDescriptionFormatter {


    private static final String THEATRE_ART_BG_LINK = """
            <a class="theatre_art_ticket" href="%s">theatre.art.bg ticket</a>
            """;

    private static final String HTML_TEMPLATE = """
            <a class="play_link" href="%s">theatre.art.bg</a>
            <br/>
            %s
            <p class="crew">%s</p>
            <br/>
            <b class="title">%s</b><b class="rating">%s</b>
            <br/>
            <div class="description">%s</div>
            <br/>
            Последно обвновено: <b class="last_sync_time">%s</b>
            """;

    public static String getHtmlEventDescription(ImmutableGoogleCalendarEventSchedulerPayload payload) {
        var theatreArtBgTicketLink = payload.getTheatreArtBgTicket()
                .map(link -> String.format(THEATRE_ART_BG_LINK, link))
                .orElse("");

        return String.format(HTML_TEMPLATE,
                             payload.getUrl(),
                             theatreArtBgTicketLink,
                             payload.getCrew(),
                             payload.getTitle(), payload.getRating(),
                             payload.getDescription(),
                             payload.getLastUpdated().toString());
    }

}
