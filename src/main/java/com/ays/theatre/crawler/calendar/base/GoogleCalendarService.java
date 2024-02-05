package com.ays.theatre.crawler.calendar.base;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.utils.Constants;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
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
import com.google.api.services.calendar.model.Events;

import jakarta.inject.Singleton;

// https://developers.google.com/calendar/api/quickstart/java
@Singleton
public class GoogleCalendarService {
    private static final Logger LOG = Logger.getLogger(GoogleCalendarService.class);

    private static final Retryer<Event> GOOGLE_CALENDAR_RETRYER = RetryerBuilder.<Event>newBuilder()
            .retryIfExceptionOfType(IOError.class)
            .retryIfException()
            .withStopStrategy(StopStrategies.neverStop())
            .withWaitStrategy(WaitStrategies.exponentialWait())
            .build();

    private static final String CALENDAR_ID =
            "ffa06350dba9afc747046f35509b4c36c31d7b2c96db88741340ec91aa28692a@group.calendar.google.com";

    private static final String CREDENTIALS_FILE_PATH = "/google-calendar-credentials.json";


    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_EVENTS);

    private static final Calendar CALENDAR_SERVICE = getCalendar();


    // utl:https://drive.google.com/open?id=1nghWfxYIvBBxzSiqBBdxhVI-yOvirxp9&authuser=0
    // title:IMG_0039.HEIC
    // mimetype:image/heif
    // https://developers.google.com/calendar/api/v3/reference/events/insert
    public Event createCalendarEvent(ImmutableGoogleCalendarEventSchedulerPayload payload) {
        var eventDescription = GoogleCalendarDescriptionFormatter.getHtmlEventDescription(payload);
        var event = getEvent(payload, eventDescription);

        try {
            LOG.info(String.format("Creating calendar event for %s at %s", payload.getUrl(), payload.getStartTime()));
            var googleCalendarEvent = createEvent(event);
            LOG.info(String.format("Created calendar event for %s at %s", payload.getUrl(), payload.getStartTime()));
            return googleCalendarEvent;
        } catch (ExecutionException | RetryException ex) {
            throw new RuntimeException(String.format("[%s] Failed to create Google Calendar event for %s-%s",
                                                     payload.getUrl(), payload.getTitle(), payload.getTheatre()), ex);
        }
    }

    private Event createEvent(Event event) throws ExecutionException, RetryException {
        return GOOGLE_CALENDAR_RETRYER.call(
                () -> CALENDAR_SERVICE.events().insert(CALENDAR_ID, event).execute());
    }

    private Event getEvent(ImmutableGoogleCalendarEventSchedulerPayload payload, String eventDescription) {
        return new Event()
                .setSummary(payload.getTitle())
                .setLocation(payload.getTheatre())
                .setDescription(eventDescription)
                .setStart(getEventDateTime(payload.getStartTime()))
                .setEnd(getEventDateTime(payload.getStartTime().plusMinutes(10)));
    }

    public Event getEventById(String eventId) {
        try {
            var result = CALENDAR_SERVICE.events().get(CALENDAR_ID, eventId).execute();
            // FIXME
            LOG.info(String.format("EVENT %s\n%s\n%s\n%s",result.getId(), result.getSummary(),
                                   result.getLocation(), result.getDescription()));

            // FIXME
            var attachments = result.getAttachments();
            if (attachments != null) {
                attachments
                        .forEach(a -> LOG.info(String.format("utl:%s\ntitle:%s\nmimetype:%s",
                                                             a.getFileUrl(), a.getTitle(), a.getMimeType())));
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // https://developers.google.com/calendar/api/v3/reference/events/list
    public ArrayList<Event> getAllEvents(OffsetDateTime offsetDateTime) {
        var allEvents = new ArrayList<Event>();
        String pageToken = null;
        do {
            Events events = list(pageToken, offsetDateTime);
            allEvents.addAll(events.getItems());
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        return allEvents;
    }

    private Events list(String pageToken, OffsetDateTime offsetDateTime) {
        try {
            return CALENDAR_SERVICE.events()
                    .list(CALENDAR_ID)
                    .setPageToken(pageToken)
                    .setSingleEvents(true)
                    // Order by the start date/time (ascending). This is only available when querying single events
                    // (i.e. the parameter singleEvents is True)
                    .setOrderBy("startTime")
                    // Upper bound (exclusive) for an event's start time to filter by. Optional. The default is not to
                    // filter by start time. Must be an RFC3339 timestamp with mandatory time zone offset, for example,
                    // 2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but are ignored.
                    // If timeMin is set, timeMax must be greater than timeMin.
                    .setTimeMin(new DateTime(offsetDateTime.toInstant().toEpochMilli()))
                    .execute();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void delete(List<Event> events) {
        events.forEach(event -> {
            try {
                GOOGLE_CALENDAR_RETRYER.call(() -> {
                    CALENDAR_SERVICE.events().delete(CALENDAR_ID, event.getId()).execute();
                    return event;
                });
            } catch (ExecutionException | RetryException ex) {
                LOG.error("Failed to delete event " + event.getId(), ex);
            }
        });

    }

    private EventDateTime getEventDateTime(OffsetDateTime dateTime) {
        return new EventDateTime()
                .setDateTime(new DateTime(dateTime.toInstant().toEpochMilli()))
                .setTimeZone(Constants.TIMEZONE);
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
