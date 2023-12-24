package com.ays.theatre.crawler.theatreartbg.service;

import static com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants.BASE_URL;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedDayMetadata;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedPlayMetadata;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgScraperService {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgScraperService.class);

    public Optional<ImmutableTheatreArtBgExtractedDayMetadata> extractPlayData(Page page) {
        page.waitForSelector("td.left");
        var tableWithPlays = page.locator("td.left").locator("#left");

        var rows = tableWithPlays.locator(":scope div.padding div.postanovka tr");
        var rowCount = rows.count();
        LOG.info("logs " + rowCount);

        if (rowCount == 0) {
            return Optional.empty();
        }

        var plays = rows.locator(":scope div.text");
        var playsCount = plays.count();
        LOG.info("play: " + playsCount);

        var url = page.url();
        var dayOfMonth = getDayOfMonth(url);
        var playsMetadata = new ArrayList<ImmutableTheatreArtBgExtractedPlayMetadata>();

        for (int i = 0; i < playsCount; i++) {
            playsMetadata.addAll(getPlayMetadata(plays.nth(i), url));
        }

        return Optional.of(ImmutableTheatreArtBgExtractedDayMetadata.builder()
                .addAllPlaysMetadata(playsMetadata)
                .day(dayOfMonth)
                .build());
    }

    private int getDayOfMonth(String url) {
        int dayOfMonth;
        if (url.equals(BASE_URL)) {
            // the base url is for the current day and so today's date can be queries via the calendar
            Calendar cal = Calendar.getInstance();
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        } else {
            String[] dateParts = getDayFromUrl(url);
            dayOfMonth = Integer.parseInt(dateParts[2]);
        }
        return dayOfMonth;
    }

    private String[] getDayFromUrl(String url) {
        var parts = url.split("______");
        if (parts.length == 0) {
            throw new RuntimeException("Did not match the data from the url");
        }
        if (parts.length > 3) {
            throw new RuntimeException("Matched more than 3 group in the url");
        }
        String dateFromUrl = parts[1];
        String[] dateParts = dateFromUrl.split("-");
        if (dateParts.length < 3) {
            throw new RuntimeException(String.format("Date %s from URL is not in the form yyyy-mm-dd", url));
        }
        return dateParts;
    }

    private List<ImmutableTheatreArtBgExtractedPlayMetadata> getPlayMetadata(Locator play, String url) {
        var link = play.locator(":scope h2 a");
        var href = link.getAttribute("href");
        var title = link.getAttribute("title");

        var theatre = play.locator(":scope a>>nth=1");
        var theatreName = theatre.innerText();

        var dates = play.locator(":scope div.date strong");
        return dates.all().stream().map(date -> {
            var hourMinute = date.textContent().split(" ")[0].split("\\.");
            var hour = Integer.parseInt(hourMinute[0]);
            var minute = Integer.parseInt(hourMinute[1]);
            LOG.info(String.format("%s: %s @ %d:%d, %s", title, href, hour, minute, theatreName));

            return ImmutableTheatreArtBgExtractedPlayMetadata.builder()
                    .title(title)
                    .url(BASE_URL + href)
                    .minute(minute)
                    .hour(hour)
                    .theatre(theatreName)
                    .build();
        }).toList();
    }
}
