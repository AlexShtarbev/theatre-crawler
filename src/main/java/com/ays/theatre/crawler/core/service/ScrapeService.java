package com.ays.theatre.crawler.core.service;

import java.time.OffsetDateTime;

public interface ScrapeService<T> {

    void scrape(T obj, String url, OffsetDateTime scrapeStartTime);
}
