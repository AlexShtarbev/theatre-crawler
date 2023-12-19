
package com.ays.theatre.crawler.global.dao;

import static com.ays.theatre.crawler.Configuration.CUSTOM_DSL;

import java.util.List;

import org.jooq.DSLContext;

import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class TheatrePlayDao {

    @Inject
    @Named(CUSTOM_DSL)
    DSLContext dslContext;

    public void merge(List<TheatrePlayRecord> records) {
        dslContext.batchMerge(records).execute();
    }
}
