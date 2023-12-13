package com.ays.theatre.crawler.theatreartbg.model;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtBgCalendar {

    int getMonth();

    int getYear();

    String getUrl();
}
