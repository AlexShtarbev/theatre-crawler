package com.ays.theatre.crawler.global.service;

import com.microsoft.playwright.Page;
import org.jsoup.nodes.Document;

public interface TheatreService<T> {

    void scrape(T obj, String url);
}
