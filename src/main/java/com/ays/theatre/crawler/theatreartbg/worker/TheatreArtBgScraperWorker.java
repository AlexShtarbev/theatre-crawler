
package com.ays.theatre.crawler.theatreartbg.worker;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedDayMetadata;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgScraperService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

// https://playwright.dev/java/docs/multithreading
public class TheatreArtBgScraperWorker implements Runnable {

    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperWorker.class);
    public static final int PAGE_DEFAULT_TIMEOUT = 1200000;

    private final TheatreArtBgScraperService theatreArtBgScraperService;
    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final TheatrePlayDao theatrePlayDao;
    private final Browser browser;

    public TheatreArtBgScraperWorker(TheatreArtBgScraperService theatreArtBgScraperService,
                                     ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                                     TheatrePlayDao theatrePlayDao) {
        this.theatreArtBgScraperService = theatreArtBgScraperService;
        this.browser = Playwright.create().webkit().launch();
        this.theatrePlayDao = theatrePlayDao;
        this.queue = queue;
    }

    @Override
    public void run() {
        LOG.info("Starting worker");
        while(true) {
            ImmutableTheatreArtQueuePayload payload;
            do {
                payload = queue.poll();
            } while (payload == null);

            LOG.info(String.format("==> Visiting URL: %s", payload.getUrl()));
            try (var page = browser.newPage()) {
                page.setDefaultTimeout(PAGE_DEFAULT_TIMEOUT);
                page.navigate(payload.getUrl());
                var maybeMetadata = theatreArtBgScraperService.extractPlayData(page);
                if (maybeMetadata.isEmpty()) {
                    continue;
                }

                var metadata = maybeMetadata.get();
                var calendar = payload.getCalendar();
                var playMetadataRecords = getRecords(metadata, calendar);
                theatrePlayDao.merge(playMetadataRecords);
                LOG.info(String.format("Finished scraping: %s", payload.getUrl()));
            } catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }

    private List<TheatrePlayRecord> getRecords(
            ImmutableTheatreArtBgExtractedDayMetadata metadata, ImmutableTheatreArtBgCalendar calendar) {
        return metadata.getPlaysMetadata()
                .stream()
                .map(play -> {
                    var localDateTime = LocalDateTime.of(calendar.getYear(), calendar.getMonth(),
                                                 metadata.getDay(), play.getHour(), play.getMinute());

                    return new TheatrePlayRecord()
                            .setTitle(play.getTitle())
                            .setUrl(play.getUrl())
                            .setTheatre(play.getTheatre())
                            .setDate(OffsetDateTime.of(localDateTime, ZoneOffset.UTC))
                            .setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));
                }).toList();
    }
}
