
package com.ays.theatre.crawler.theatreartbg.model;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtBgExtractedPlayMetadata {
    String getTheatre();
    String getTitle();
    String getUrl();
    int getHour();
    int getMinute();
}
