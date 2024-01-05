package com.ays.theatre.crawler.core.model;

import java.util.Optional;

import org.immutables.value.Value;

import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;

@Value.Immutable
public interface TheatrePlayObject {
    Optional<Throwable> getError();

    ChangeAction getChangeAction();

    TheatrePlayRecord getRecord();
}
