package com.ays.theatre.crawler.calendar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import jakarta.inject.Singleton;

// https://developers.google.com/calendar/api/quickstart/java
@Singleton
public class GoogleCalendarService {

    private static final String CALENDAR_ID =
            "ffa06350dba9afc747046f35509b4c36c31d7b2c96db88741340ec91aa28692a@group.calendar.google.com";

    private static final String CREDENTIALS_FILE_PATH = "/google-calendar-credentials.json";

    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";


    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_EVENTS);

    private static final Calendar CALENDAR_SERVICE = getCalendar();

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    public void createCalendarEvent(String title, String location, String url, OffsetDateTime dateTime) {
        Event event = new Event()
                .setSummary(title)
                .setLocation(location)
                .setDescription(url)
                .setStart(getEventDateTime(dateTime))
                .setEnd(getEventDateTime(dateTime.plusMinutes(10)));

        try {
            CALENDAR_SERVICE.events().insert(CALENDAR_ID, event).execute();
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to create Google Calendar event for %s-%s", url, title),
                                       ex);
        }
    }

    private static EventDateTime getEventDateTime(OffsetDateTime dateTime) {
        return new EventDateTime()
                .setDateTime(new DateTime(dateTime.toInstant().toEpochMilli()))
                .setTimeZone(dateTime.toZonedDateTime().getZone().toString());
    }

    private static Calendar getCalendar() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            var credentials = getCredentials();

            return new Calendar.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    credentials
            )
                    .setApplicationName("Theater Crawler")
                    .build();
        } catch (IOException | GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static GoogleCredential getCredentials()
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        return GoogleCredential.fromStream(in).createScoped(SCOPES);
    }

}
