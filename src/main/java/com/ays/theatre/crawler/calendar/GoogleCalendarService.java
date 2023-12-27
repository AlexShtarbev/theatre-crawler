package com.ays.theatre.crawler.calendar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.List;

import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(GoogleCalendarService.class);

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
    public void createCalendarEvent(String title, String location, String url, OffsetDateTime dateTime) {
        Event event = new Event()
                .setSummary(title)
                .setLocation(location)
                .setDescription(" <h1> 1984  -ТА ПО ДЖОРДЖ ОРУЕЛ</h1>\n"
                                + "\t\t\t\t\t\t        \t\t\t\t\t\t    <div class=\"theatre\"><a href=\"нов-театър-ндк___160\">Нов Театър - НДК</a></div>\n"
                                + "\t\t\t\t\t\t        \t\t\t\t\t\t    <br>\n"
                                + "\t\t\t\t\t\t    <font class=\"textver10\"><b>Режисьор и сценарист:</b></font> <a class=\"post\" href=\"бойко-илиев__342\" onmouseover=\"showToolTip(event,'<center><br><img src=\\'//theatre.peakview.bg/theatre/photos/SMALL1365670799Boyko-Iliev.JPG\\' width=100><br>Бойко Илиев  </center>');return false;\" onmouseout=\"hideToolTip()\">Бойко Илиев   </a><br><font class=\"textver10\"><b>Сценография:</b></font> <a class=\"post\" href=\"теодор-даскалов__2494\">Теодор Даскалов </a><br><font class=\"textver10\"><b>:</b></font> <a class=\"post\" href=\"жанет-иванова__170\" onmouseover=\"showToolTip(event,'<center><br><img src=\\'//theatre.peakview.bg/theatre/photos/SMALL132300048521Janet-Ivanova.jpg\\' width=100><br>Жанет Иванова  </center>');return false;\" onmouseout=\"hideToolTip()\">Жанет Иванова   </a><br>\t\t\t\t\t\t        \t\t\t\t\t\t    <font class=\"textver10\"><strong>Участват: </strong></font><a href=\"веселин-калановски__6093\" class=\"post\">Веселин Калановски</a>,&nbsp;<a href=\"цветан-николов__2229\" class=\"post\" onmouseover=\"showToolTip(event,'<center><br><img src=\\'//theatre.peakview.bg/theatre/photos/SMALL131954506921tsvetan-nikolov.jpg\\' width=100><br>Цветан Николов  </center>');return false;\" onmouseout=\"hideToolTip()\">Цветан Николов</a><br><br>\n"
                                + "\n"
                                + "\t\t\t\t\t\t    \t\t\t\t\t\t    <br>\n"
                                + "\t\t\t\t\t    <span style=\"font-size:14px\"><span style=\"font-family:arial,helvetica,sans-serif\"><strong>„1984\" -та&nbsp;</strong>по Джордж Оруел</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Драматизация и режисура Бойко Илиев</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Сценография Теодор Даскалов<br>\n"
                                + "Костюми Жанета Иванова</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Музика: Любомир Денев</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Участват Петьо Горанов&nbsp;</span><span style=\"font-family:arial,helvetica,sans-serif\">в главната роля на Уинстън Смит</span><span style=\"font-family:segoe ui light,sans-serif\">, Христина Пипова, </span><span style=\"font-family:segoe ui light,sans-serif\">Цветан Николов,</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Веселин Калановски, Надя Илиева, Александрина Събева</span><br>\n"
                                + "<span style=\"font-family:segoe ui light,sans-serif\">Продукция: „Нов театър” НДК с подкрепата на Министерство на културата</span></span><br>\n"
                                + "<br>\n"
                                + "<span style=\"font-size:14px\"><span style=\"font-family:arial,helvetica,sans-serif\"><strong>„ГОЛЕМИЯТ БРАТ ТЕ НАБЛЮДАВА“</strong><br>\n"
                                + "„1984“ – повече от седемдесет години след публикуването на шедьовъра-– „ненаучна фантастика“ на Джордж Оруел, се взираме в това всевиждащо око, което ни предупреждава за „големия брат“. На неговите:&nbsp; „двумисъл“ и „новговор“ сме намерили други понятия и те са част от ежедневието ни. „По оруелски“ днес често е синоним за манипулация на общественото съзнание. Наложило ни се е да се примирим с извода на Оруел в „1984“, който гласи:<br>\n"
                                + "<strong><em>„Изборът за човечеството беше между свобода и щастие… и за по-голямата част от човечеството щастието е по-добрият избор“.</em></strong><br>\n"
                                + "И днес сме заобиколени с екрани, както героите на Джордж Оруел. &nbsp;„БИГ БРЪДЪР“, „НЕВЕЖЕСТВОТО Е СИЛА“, „ВОЙНАТА Е МИР“, „СВОБОДАТА Е РОБСТВО“… Познато ли ви е? Още във „Фермата на животните“ Оруел провидя безмилостно ни настояще. Да се откажеш да си част от стадото е по-трудно, отколкото изглежда. Историята на героя му Уинстън Смит е все&nbsp; по-стряскащо разпознаваема. За да&nbsp; се откъснеш от лудостта на това стадо, е&nbsp; необходима сила и дълбочина на характера и яснота на съзнанието. Но не е ли това единственият път, ако искаме да запазим&nbsp;&nbsp; правото да мислим, да говорим и да действаме, независимо какво ни&nbsp; диктуват от екрана…<br>\n"
                                + "Бойко Илиев – режисьор</span></span><br>\n"
                                + "<br>\n"
                                + "<br>\n"
                                + "<br>\n"
                                + "&nbsp;              <div><br></div>")
                .setStart(getEventDateTime(dateTime))
                .setEnd(getEventDateTime(dateTime.plusMinutes(10)));

        try {
            CALENDAR_SERVICE.events().insert(CALENDAR_ID, event).execute();
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to create Google Calendar event for %s-%s", url, title),
                                       ex);
        }

        try {
            var result = CALENDAR_SERVICE.events().get(CALENDAR_ID, "afmf28kv96g24siu0pqasc99m0").execute();
            result.getAttachments()
                    .forEach(a -> LOG.info(String.format("utl:%s\ntitle:%s\nmimetype:%s",
                                                         a.getFileUrl(), a.getTitle(), a.getMimeType())));

        } catch (IOException e) {
            throw new RuntimeException(e);
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
