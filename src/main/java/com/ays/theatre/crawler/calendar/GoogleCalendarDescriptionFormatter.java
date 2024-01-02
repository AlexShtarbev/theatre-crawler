package com.ays.theatre.crawler.calendar;

import com.ays.theatre.crawler.calendar.ImmutableGoogleCalendarEventSchedulerPayload;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarDescriptionFormatter {

    private static String HTML_TEMPLATE = """
            <a href="%s">theatre.art.bg</a>
            <br/>
            %s
            <br/>
            %s %s
            <br/>
            %s
            """;

    public static String getHtmlEventDescription(ImmutableGoogleCalendarEventSchedulerPayload payload) {
        return String.format(HTML_TEMPLATE,
                payload.getUrl(),
                payload.getCrew(),
                payload.getTitle(), payload.getRating(),
                payload.getDescription());
    }

}
