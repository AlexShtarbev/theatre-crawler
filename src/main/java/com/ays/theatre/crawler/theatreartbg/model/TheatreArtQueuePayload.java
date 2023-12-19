package com.ays.theatre.crawler.theatreartbg.model;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtQueuePayload {

    ImmutableTheatreArtBgCalendar getCalendar();

    String getUrl();
}
