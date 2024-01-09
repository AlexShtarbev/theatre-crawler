package com.ays.theatre.crawler.calendar.dao;

import static com.ays.theatre.crawler.Configuration.CUSTOM_DSL;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.jooq.DSLContext;

import com.ays.theatre.crawler.Tables;
import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.model.ImmutableResycRecord;
import com.ays.theatre.crawler.tables.records.GoogleCalendarEventsRecord;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarDao {

    private final DSLContext dslContext;
    private final TheatrePlayDao theatrePlayDao;
    private final GoogleCalendarScheduledEventMapper mapper;

    public GoogleCalendarDao(GoogleCalendarScheduledEventMapper mapper,
                             TheatrePlayDao theatrePlayDao,
                             @Named(CUSTOM_DSL) DSLContext dslContext) {
        this.mapper = mapper;
        this.dslContext = dslContext;
        this.theatrePlayDao = theatrePlayDao;
    }

    public int upsertEvent(String url, OffsetDateTime time, String eventId) {
        var record = new GoogleCalendarEventsRecord()
                .setUrl(url)
                .setDate(time)
                .setEventid(eventId);

        return dslContext.insertInto(Tables.GOOGLE_CALENDAR_EVENTS)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
    }

    public List<ImmutableGoogleCalendarEventSchedulerPayload> getRecords(String origin, OffsetDateTime after) {
        var playFields = Tables.THEATRE_PLAY.fields();
        var detailsFields = Tables.THEATRE_PLAY_DETAILS.fields();
        var allFields = ArrayUtils.addAll(playFields, detailsFields);
        return dslContext.select(allFields)
                .from(Tables.THEATRE_PLAY)
                .leftJoin(Tables.THEATRE_PLAY_DETAILS)
                .on(Tables.THEATRE_PLAY.URL.eq(Tables.THEATRE_PLAY_DETAILS.URL))
                .and(Tables.THEATRE_PLAY.ORIGIN.eq(Tables.THEATRE_PLAY.ORIGIN))
                .where(Tables.THEATRE_PLAY.ORIGIN.eq(origin))
                .and(Tables.THEATRE_PLAY.DATE.greaterOrEqual(after))
                .orderBy(Tables.THEATRE_PLAY.URL, Tables.THEATRE_PLAY.DATE)
                .fetch(mapper);
    }


    public void reSyncRecords(List<ImmutableResycRecord> recordPairs) {
        dslContext.transaction(configuration -> {
            var dsl = configuration.dsl();
            recordPairs.forEach(pair -> {
                theatrePlayDao.insertPlay(dsl, pair.getPlayRecord());
                theatrePlayDao.insertDetails(dsl, pair.getDetailsRecord());
            });
        });
    }

}
