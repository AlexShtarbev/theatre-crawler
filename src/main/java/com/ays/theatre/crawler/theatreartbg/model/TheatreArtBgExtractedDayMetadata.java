
package com.ays.theatre.crawler.theatreartbg.model;

import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtBgExtractedDayMetadata {

    List<TheatreArtBgExtractedPlayMetadata> getPlaysMetadata();
    int getDay();
}
