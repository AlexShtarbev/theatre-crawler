
package com.ays.theatre.crawler.global.dao;

import com.ays.theatre.crawler.Tables;
import com.ays.theatre.crawler.global.model.ChangeAction;
import com.ays.theatre.crawler.global.model.ImmutableTheatrePlayObject;
import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.ays.theatre.crawler.Configuration.CUSTOM_DSL;

@Singleton
public class TheatrePlayDao {

    @Inject
    @Named(CUSTOM_DSL)
    DSLContext dslContext;

    public List<ImmutableTheatrePlayObject> merge(List<TheatrePlayRecord> records) {
        return records.stream().map(record -> dslContext.transactionResult(configuration -> {
            var builder = ImmutableTheatrePlayObject.builder().record(record);
            try {
                var fetchedRecord = fetchRecord(record, configuration);
                if (fetchedRecord.isPresent()) {
                    builder = builder.changeAction(ChangeAction.NONE);
                } else {
                    upsert(configuration.dsl(), record);
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

    public Optional<TheatrePlayRecord> getPlayFromUrlAndDate(String url, OffsetDateTime dateTime) {
        return Optional.ofNullable(
                        dslContext.selectFrom(Tables.THEATRE_PLAY)
                                .where(Tables.THEATRE_PLAY.URL.eq(url))
                                .and(Tables.THEATRE_PLAY.DATE.eq(dateTime))
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

    public int upsertDetails(TheatrePlayDetailsRecord record) {
        return dslContext.insertInto(Tables.THEATRE_PLAY_DETAILS)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
    }

    public Optional<TheatrePlayDetailsRecord> getTheatrePlayDetails(String url) {
        return Optional.ofNullable(dslContext.selectFrom(Tables.THEATRE_PLAY_DETAILS)
                .where(Tables.THEATRE_PLAY_DETAILS.URL.eq(url))
                .fetchOneInto(TheatrePlayDetailsRecord.class));
    }

    public List<String> getTheatrePlaysByOriginAndDatePaged(String origin, OffsetDateTime dateTime) {
        return dslContext.selectDistinct(Tables.THEATRE_PLAY.URL)
                .from(Tables.THEATRE_PLAY)
                .where(Tables.THEATRE_PLAY.ORIGIN.eq(origin))
                .and(Tables.THEATRE_PLAY.DATE.greaterOrEqual(dateTime))
                .orderBy(Tables.THEATRE_PLAY.URL.asc())
                .fetchInto(String.class);
    }
}
