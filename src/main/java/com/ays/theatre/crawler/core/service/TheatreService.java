package com.ays.theatre.crawler.core.service;

public interface TheatreService<T> {

    void scrape(T obj, String url);
}
