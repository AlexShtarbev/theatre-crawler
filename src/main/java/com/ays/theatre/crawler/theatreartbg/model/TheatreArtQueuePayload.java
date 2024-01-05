package com.ays.theatre.crawler.theatreartbg.model;

import org.immutables.value.Value;

import com.ays.theatre.crawler.core.service.TheatreService;

@Value.Immutable
public interface TheatreArtQueuePayload {

    Object getObject();

    String getUrl();
}
