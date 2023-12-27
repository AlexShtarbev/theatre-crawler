
package com.ays.theatre.crawler.global.dao;

import static com.ays.theatre.crawler.Configuration.CUSTOM_DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.jooq.Configuration;
import org.jooq.DSLContext;

import com.ays.theatre.crawler.Tables;
import com.ays.theatre.crawler.global.model.ChangeAction;
import com.ays.theatre.crawler.global.model.ImmutableTheatrePlayObject;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayMetadataRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class TheatrePlayDao {

    @Inject
    @Named(CUSTOM_DSL)
    DSLContext dslContext;

    @Inject
    ObjectMapper objectMapper;

    public List<ImmutableTheatrePlayObject> merge(List<TheatrePlayRecord> records) {
        return records.stream().map(record -> dslContext.transactionResult(configuration -> {
            var builder = ImmutableTheatrePlayObject.builder().record(record);
            try {
                var fetchedRecord = fetchRecord(record, configuration);
                if (fetchedRecord.isPresent()) {
                    builder = builder.changeAction(ChangeAction.NONE);
                } else {
                    upsert(configuration.dsl(), record);
                    upsertMeta(configuration.dsl(), record);
                    builder = builder.changeAction(ChangeAction.NEW);
                }
            } catch (Exception ex) {
                builder = builder.changeAction(ChangeAction.ERROR)
                        .error(Optional.of(ex));
            }

            return  builder.build();
        })).toList();
    }

    private static Optional<TheatrePlayRecord> fetchRecord(TheatrePlayRecord record, Configuration configuration) {
        return Optional.ofNullable(
                configuration.dsl().selectFrom(Tables.THEATRE_PLAY)
                        .where(Tables.THEATRE_PLAY.URL.eq(record.getUrl()))
                        .and(Tables.THEATRE_PLAY.DATE.eq(record.getDate()))
                        .fetchOne())
                .map(r -> r.into(TheatrePlayRecord.class));
    }

    private int upsert(DSLContext dslContext, TheatrePlayRecord record) {
        return dslContext.insertInto(Tables.THEATRE_PLAY)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
    }

    private int upsertMeta(DSLContext dslContext, TheatrePlayRecord record) {
        var metaRecord = new TheatrePlayMetadataRecord();
        metaRecord.setUrl(record.getUrl());
        metaRecord.setDate(record.getDate());
        metaRecord.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));

        return dslContext.insertInto(Tables.THEATRE_PLAY_METADATA)
                .set(metaRecord)
                .onDuplicateKeyUpdate()
                .set(metaRecord)
                .execute();
    }

    public int upsertDetails(String url, String description) {
        var record = new TheatrePlayDetailsRecord().setUrl(url).setDescription(description);
        return dslContext.insertInto(Tables.THEATRE_PLAY_DETAILS).
                set(record)
                .onConflictDoNothing()
                .execute();
    }
}
