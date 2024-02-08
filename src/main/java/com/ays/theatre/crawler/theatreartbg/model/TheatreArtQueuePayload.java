package com.ays.theatre.crawler.theatreartbg.model;

import java.time.OffsetDateTime;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtQueuePayload {

    Object getObject();

    String getUrl();

    OffsetDateTime getScrapingStartTime();
}
