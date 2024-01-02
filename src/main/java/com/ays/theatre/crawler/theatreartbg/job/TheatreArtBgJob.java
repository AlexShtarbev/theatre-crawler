
package com.ays.theatre.crawler.theatreartbg.job;

import com.ays.theatre.crawler.global.Constants;
import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.service.LatchService;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.theatreartbg.service.TheatreArtBgDayService;
import com.ays.theatre.crawler.utils.PageUtils;
import io.quarkus.logging.Log;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Singleton
public class TheatreArtBgJob {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgJob.class);

    private final ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;
    private final TheatreArtBgDayService service;
    private final LatchService latchService;
    private final  TheatrePlayDao theatrePlayDao;

    public TheatreArtBgJob(ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue,
                           TheatreArtBgDayService service,
                           LatchService latchService,
                           TheatrePlayDao theatrePlayDao) {
        this.queue = queue;
        this.service = service;
        this.latchService = latchService;
        this.theatrePlayDao = theatrePlayDao;
    }

    public void run() {
        LOG.info("Starting Theatre.art.bg job");
        LOG.info("Navigating to " + Constants.THEATRE_ART_BG_BASE_URL);
        var doc = PageUtils.navigateWithRetry(Constants.THEATRE_ART_BG_BASE_URL);
        try {
            var calendar = service.getCalendar(doc);
            var dayUrls = getDayUrls(calendar);
            latchService.init(Constants.THEATRE_ART_BG_DAY_LATCH, dayUrls.size());
            queue.addAll(dayUrls);
            latchService.await(Constants.THEATRE_ART_BG_DAY_LATCH);
            Log.info("Done with scraping for day urls");

            var allPlayRecords = theatrePlayDao.getTheatrePlaysByOriginAndDatePaged(Constants.THEATRE_ART_BG_ORIGIN,
                    OffsetDateTime.now());

            var playPayloads = allPlayRecords.stream().map(url ->
                ImmutableTheatreArtQueuePayload.builder().url(url)
                        .object(ImmutableTheatreArtBgPlayObject.builder().build())
                        .build()
            ).toList();
            latchService.init(Constants.THEATRE_ART_BG_PLAY_LATCH, playPayloads.size());
            queue.addAll(playPayloads);
            latchService.await(Constants.THEATRE_ART_BG_PLAY_LATCH);
            Log.info("Done scraping play urls");
        } catch (Exception ex) {
            LOG.error("Failed to get calendar", ex);
        }
    }

    private List<ImmutableTheatreArtQueuePayload> getDayUrls(List<ImmutableTheatreArtBgCalendar> calendar) {
        return calendar.stream().flatMap(cal -> {
            LOG.info(String.format("---Loading all other days for %d %d: %s---",
                    cal.getMonth(), cal.getYear(), cal.getUrl()));

            var monthPage = PageUtils.navigateWithRetry(cal.getUrl());
            var nextDays = service.getAllDaysOfMonthsUrls(monthPage);
            var allDaysLinks = new ArrayList<>(nextDays);
            allDaysLinks.add(cal.getUrl());

            return allDaysLinks.stream().map(url -> getPayload(cal, url));
        }).toList();
    }

    private ImmutableTheatreArtQueuePayload getPayload(ImmutableTheatreArtBgCalendar cal, String url) {
        return ImmutableTheatreArtQueuePayload.builder().url(url).object(cal).build();
    }
}
