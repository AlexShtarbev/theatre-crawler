package com.ays.theatre.crawler.global.service;

import com.microsoft.playwright.Page;

public interface TheatreService<T> {

    void scrape(T obj, Page page);
}
