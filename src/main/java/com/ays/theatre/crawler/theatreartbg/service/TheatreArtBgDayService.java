package com.ays.theatre.crawler.theatreartbg.service;

import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.model.ImmutableTheatrePlayObject;
import com.ays.theatre.crawler.core.service.LatchService;
import com.ays.theatre.crawler.core.service.TheatreService;
import com.ays.theatre.crawler.core.utils.Origin;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.model.*;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedDayMetadata;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedPlayMetadata;
import com.ays.theatre.crawler.core.utils.DateUtils;
import com.ays.theatre.crawler.core.utils.PageUtils;

import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.ays.theatre.crawler.core.utils.Constants.THEATRE_ART_BG_BASE_URL;

@Singleton
public class TheatreArtBgDayService implements TheatreService<ImmutableTheatreArtBgCalendar> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgDayService.class);
    public static final String HREF = "href";

    private final TheatrePlayDao theatrePlayDao;
    private final LatchService latchService;

    public TheatreArtBgDayService(TheatrePlayDao theatrePlayDao, LatchService latchService) {
        this.theatrePlayDao = theatrePlayDao;
        this.latchService = latchService;
    }

    public List<ImmutableTheatreArtBgCalendar> getCalendar(Document page) {
        var date = DateUtils.getDateWithoutTimeUsingCalendar();
        var year = date.get(Calendar.YEAR);

        var currentMonth = page.select("div.month a").getFirst();
        var currentMonthName = currentMonth.text();

        var changeMonths = page.select("div.change_month");

        List<ImmutableTheatreArtBgCalendar> list = new ArrayList<>();

        var month = DateUtils.BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP.get(currentMonthName.toLowerCase());
        list.add(ImmutableTheatreArtBgCalendar.builder()
                .month(month)
                .year(year)
                .url(Constants.THEATRE_ART_BG_BASE_URL)
                .build());

        changeMonths.forEach(locator -> {
            var monthLink = locator.select("a").getFirst();
            var url = monthLink.attr(HREF);
            var name = monthLink.text();

            var nextMonth = DateUtils.BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP.get(name.toLowerCase());
            var nextMonthYear = year;
            if (nextMonth < month) {
                nextMonthYear++;
            }

            list.add(ImmutableTheatreArtBgCalendar.builder()
                    .month(nextMonth)
                    .year(nextMonthYear)
                    .url(Constants.THEATRE_ART_BG_BASE_URL + url)
                    .build());
        });

        return list;
    }

    public ArrayList<String> getAllDaysOfMonthsUrls(Document page) {
        var calendar = page.select("div.calendar").getFirst();
        var daysOfTheMonth = calendar.getElementsByTag("li");
        var daysOfTheMonthCount = daysOfTheMonth.size();

        // skip all days before the current one
        int index = 0;
        while (!Objects.equals(daysOfTheMonth.get(index).attr("class"), "current")) {
            index++;
        }

        // skip the current day
        index++;

        var nextLinks = new ArrayList<String>();
        for (; index < daysOfTheMonthCount; index++) {
            var linkOfEachDay = daysOfTheMonth.get(index).select("a");
            var href = linkOfEachDay.attr(HREF);
            var dayNumber = linkOfEachDay.text();
            nextLinks.add(Constants.THEATRE_ART_BG_BASE_URL + href);
            LOG.info(String.format("%s: %s", dayNumber, href));
        }

        return nextLinks;
    }

    @Override
    public void scrape(ImmutableTheatreArtBgCalendar calendar, String url) {
        var mergeResult = List.<ImmutableTheatrePlayObject>of();
        try {
            LOG.info("Will navigate to: " + url);
            var page = PageUtils.navigateWithRetry(url);
            LOG.info("Started scarping: " + url);
            var maybeMetadata = extractPlayData(page, url);
            if (maybeMetadata.isEmpty()) {
                LOG.info("count down");
                latchService.countDown(Constants.THEATRE_ART_BG_DAY_LATCH);
                return;
            }

            var playMetadataRecords = getRecords(maybeMetadata.get(), calendar);
            mergeResult = theatrePlayDao.merge(playMetadataRecords);
            LOG.info("count down");
            latchService.countDown(Constants.THEATRE_ART_BG_DAY_LATCH);
        } catch (Exception ex) {
            LOG.error(ex);
        } finally {
            runPostMergePlayScraping(mergeResult);
            LOG.info(String.format("Finished scraping: %s", url));
        }
    }

    private List<TheatrePlayRecord> getRecords(
            ImmutableTheatreArtBgExtractedDayMetadata metadata, ImmutableTheatreArtBgCalendar calendar) {
        return metadata.getPlaysMetadata()
                .stream()
                .map(play -> {
                    try {
                        var localDateTime = getOffsetDateTime(metadata, calendar, play);
                        return getTheatrePlayRecord(play, localDateTime);
                    } catch (Exception ex) {
                        LOG.error(ex);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
    }

    private static OffsetDateTime getOffsetDateTime(ImmutableTheatreArtBgExtractedDayMetadata metadata,
                                                    ImmutableTheatreArtBgCalendar calendar,
                                                    TheatreArtBgExtractedPlayMetadata play) {
        return DateUtils.toOffsetDateTime(calendar.getYear(),
                calendar.getMonth(),
                metadata.getDay(),
                play.getHour(),
                play.getMinute());
    }

    private TheatrePlayRecord getTheatrePlayRecord(TheatreArtBgExtractedPlayMetadata play, OffsetDateTime localDateTime) {
        return new TheatrePlayRecord()
                .setTitle(play.getTitle())
                .setUrl(play.getUrl())
                .setTheatre(play.getTheatre())
                .setOrigin(Origin.THEATRE_ART_BG.getOrigin())
                .setDate(localDateTime)
                .setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void runPostMergePlayScraping(List<ImmutableTheatrePlayObject> mergeResult) {
        mergeResult.forEach(object -> {
            if (object.getError().isPresent()) {
                LOG.error(String.format("Failed merge for %s-%s", object.getRecord().getUrl(),
                                        object.getRecord().getDate()), object.getError().get());
                return;
            }

            LOG.info(String.format("Merge result for %s-%s: %s", object.getRecord().getUrl(),
                                   object.getRecord().getDate(), object.getChangeAction()));
        });
    }

    public Optional<ImmutableTheatreArtBgExtractedDayMetadata> extractPlayData(Document page, String url) {
        var tableWithPlays = page.select("td.left").select("#left");

        var rows = tableWithPlays.select("div.padding div.postanovka tr");
        var rowCount = rows.size();
        LOG.info("logs " + rowCount);

        if (rowCount == 0) {
            return Optional.empty();
        }

        var plays = rows.select("div.text");
        var playsCount = plays.size();
        LOG.info("play: " + playsCount);

        var dayOfMonth = getDayOfMonth(url);
        var playsMetadata = new ArrayList<ImmutableTheatreArtBgExtractedPlayMetadata>();

        for (int i = 0; i < playsCount; i++) {
            playsMetadata.addAll(getPlayMetadata(plays.get(i)));
        }

        return Optional.of(ImmutableTheatreArtBgExtractedDayMetadata.builder()
                .addAllPlaysMetadata(playsMetadata)
                .day(dayOfMonth)
                .build());
    }

    private int getDayOfMonth(String url) {
        int dayOfMonth;
        if (url.equals(THEATRE_ART_BG_BASE_URL)) {
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

    private List<ImmutableTheatreArtBgExtractedPlayMetadata> getPlayMetadata(Element play) {
        var link = play.select("h2 a");
        var href = link.attr("href");
        var title = link.attr("title");

        var theatre = play.select("a").get(1);
        var theatreName = theatre.text();

        var dates = play.select("div.date strong");
        return dates.stream().map(date -> {
            var hourMinute = date.text().split(" ")[0].split("\\.");
            var hour = Integer.parseInt(hourMinute[0]);
            var minute = Integer.parseInt(hourMinute[1]);
            LOG.info(String.format("%s: %s @ %d:%d, %s", title, href, hour, minute, theatreName));

            return ImmutableTheatreArtBgExtractedPlayMetadata.builder()
                    .title(title)
                    .url(THEATRE_ART_BG_BASE_URL + href)
                    .minute(minute)
                    .hour(hour)
                    .theatre(theatreName)
                    .build();
        }).toList();
    }
}
