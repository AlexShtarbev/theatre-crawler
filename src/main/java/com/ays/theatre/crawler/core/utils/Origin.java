package com.ays.theatre.crawler.core.utils;

public enum Origin {
    THEATRE_ART_BG("theatre.art.bg");

    private final String origin;

    Origin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return this.origin;
    }
}
