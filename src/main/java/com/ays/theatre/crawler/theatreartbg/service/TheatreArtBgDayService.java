package com.ays.theatre.crawler.theatreartbg.service;

import com.ays.theatre.crawler.global.dao.TheatrePlayDao;
import com.ays.theatre.crawler.global.model.ChangeAction;
import com.ays.theatre.crawler.global.model.ImmutableTheatrePlayObject;
import com.ays.theatre.crawler.global.service.TheatreService;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;
import com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedDayMetadata;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgExtractedPlayMetadata;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgPlayObject;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;
import com.ays.theatre.crawler.utils.DateUtils;
import com.ays.theatre.crawler.utils.PageUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ays.theatre.crawler.Configuration.UNIQUE_PLAY_URL_SET;
import static com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants.BASE_URL;

@Singleton
public class TheatreArtBgDayService implements TheatreService<ImmutableTheatreArtBgCalendar> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgDayService.class);
    public static final String HREF = "href";
    public static final String SCOPE_A = "a";

    @Inject
    TheatrePlayDao theatrePlayDao;

    @Inject
    @Named(UNIQUE_PLAY_URL_SET)
    Set<String> playsToVisitSet;

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

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
                .url(TheatreArtBgConstants.BASE_URL)
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
                    .url(TheatreArtBgConstants.BASE_URL + url)
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
            nextLinks.add(TheatreArtBgConstants.BASE_URL + href);
            LOG.info(String.format("%s: %s", dayNumber, href));
        }

        return nextLinks;
    }

    @Override
    public void scrape(ImmutableTheatreArtBgCalendar calendar, String url) {
        try {
            LOG.info("Will navigate to: " + url);
            var page = PageUtils.navigateWithRetry(url);
            LOG.info("Started scarping: " + url);
            var maybeMetadata = extractPlayData(page, url);
            if (maybeMetadata.isEmpty()) {
                return;
            }

            var playMetadataRecords = getRecords(maybeMetadata.get(), calendar);
            var mergeResult = theatrePlayDao.merge(playMetadataRecords);
            runPostMergePlayScraping(mergeResult);
            LOG.info(String.format("Finished scraping: %s", url));
        } catch (Exception ex) {
            LOG.error(ex);
        }
    }

    private List<TheatrePlayRecord> getRecords(
            ImmutableTheatreArtBgExtractedDayMetadata metadata, ImmutableTheatreArtBgCalendar calendar) {
        return metadata.getPlaysMetadata()
                .stream()
                .map(play -> {
                    try {
                        var localDateTime = DateUtils.toOffsetDateTime(calendar.getYear(), calendar.getMonth(),
                                metadata.getDay(), play.getHour(), play.getMinute());
                        return new TheatrePlayRecord()
                                .setTitle(play.getTitle())
                                .setUrl(play.getUrl())
                                .setTheatre(play.getTheatre())
                                .setDate(localDateTime);
                    } catch (Exception ex) {
                        LOG.error(ex);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
    }

    private void runPostMergePlayScraping(List<ImmutableTheatrePlayObject> mergeResult) {
        mergeResult.forEach(object -> {
            if (object.getError().isPresent()) {
                LOG.error(String.format("Failed merge for %s-%s", object.getRecord().getUrl(),
                                        object.getRecord().getDate()), object.getError().get());
            } else {
                LOG.info(String.format("Merge result for %s-%s: %s", object.getRecord().getUrl(),
                                       object.getRecord().getDate(), object.getChangeAction()));
                if (object.getChangeAction().equals(ChangeAction.NEW)) {
                    var url = object.getRecord().getUrl();
                    if (playsToVisitSet.contains(url)) {
                        return;
                    }
                    playsToVisitSet.add(url);
                    queue.add(ImmutableTheatreArtQueuePayload.builder()
                                      .url(object.getRecord().getUrl())
                                      .object(ImmutableTheatreArtBgPlayObject.builder().build())
                                      .build());

                }
            }
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
                    .url(BASE_URL + href)
                    .minute(minute)
                    .hour(hour)
                    .theatre(theatreName)
                    .build();
        }).toList();
    }
}
