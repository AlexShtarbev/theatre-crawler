package com.ays.theatre.crawler.calendar.dao;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.jooq.Record;
import org.jooq.RecordMapper;

import com.ays.theatre.crawler.calendar.model.ImmutableGoogleCalendarEventSchedulerPayload;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;

import jakarta.inject.Singleton;

@Singleton
public class GoogleCalendarScheduledEventMapper
        implements RecordMapper<Record, ImmutableGoogleCalendarEventSchedulerPayload> {

        @Override
        public ImmutableGoogleCalendarEventSchedulerPayload map(Record record) {
            var playRecord = record.into(TheatrePlayRecord.class);
            var detailsRecord = record.into(TheatrePlayDetailsRecord.class);
            return getEventSchedulerPayload(playRecord, detailsRecord);
        }

    private static ImmutableGoogleCalendarEventSchedulerPayload getEventSchedulerPayload(
            TheatrePlayRecord playRecord,
            TheatrePlayDetailsRecord playDetailsRecord) {

        return ImmutableGoogleCalendarEventSchedulerPayload.builder()
                .title(playRecord.getTitle())
                .theatre(playRecord.getTheatre())
                .startTime(playRecord.getDate())
                .url(playRecord.getUrl())
                .theatreArtBgTicket(Optional.ofNullable(playRecord.getTicketsUrl()))
                .crew(playDetailsRecord.getCrew())
                .description(playDetailsRecord.getDescription())
                .rating(playDetailsRecord.getRating())
                .lastUpdated(playRecord.getLastUpdated())
                .build();
    }
}
