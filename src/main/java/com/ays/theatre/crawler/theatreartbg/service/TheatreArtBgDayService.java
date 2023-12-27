package com.ays.theatre.crawler.theatreartbg.service;

import static com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants.BASE_URL;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

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
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgDayService implements TheatreService<ImmutableTheatreArtBgCalendar> {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgDayService.class);
    private static final String HREF = "href";
    private static final String SCOPE_A = ":scope a";

    @Inject
    TheatrePlayDao theatrePlayDao;

    @Inject
    Set<Pair<String, OffsetDateTime>> playsToVisitSet;

    @Inject
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> queue;

    public List<ImmutableTheatreArtBgCalendar> getCalendar(Page page) {
        var date = DateUtils.getDateWithoutTimeUsingCalendar();
        var year = date.get(Calendar.YEAR);

        var currentMonth = page.locator("div.month a");
        var currentMonthName = currentMonth.innerText();

        var changeMonths = page.locator("div.change_month");

        List<ImmutableTheatreArtBgCalendar> list = new ArrayList<>();

        var month = DateUtils.BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP.get(currentMonthName.toLowerCase());
        list.add(ImmutableTheatreArtBgCalendar.builder()
                         .month(month)
                         .year(year)
                         .url(TheatreArtBgConstants.BASE_URL)
                         .build());

        changeMonths.all().forEach(locator -> {
            var monthLink = locator.locator(SCOPE_A);
            var url = monthLink.getAttribute(HREF);
            var name = monthLink.innerText();

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

    public ArrayList<String> getAllDaysOfMonthsUrls(Page page) {
        var calendar = page.locator("div.calendar");
        var daysOfTheMonth = calendar.locator(":scope li");
        var daysOfTheMonthCount = daysOfTheMonth.count();

        // skip all days before the current one
        int index = 0;
        daysOfTheMonth.nth(index).getAttribute("class");
        while (!Objects.equals(daysOfTheMonth.nth(index).getAttribute("class"), "current")) {
            index++;
        }

        // skip the current day
        index++;

        var nextLinks = new ArrayList<String>();
        for (; index < daysOfTheMonthCount; index++) {
            var linkOfEachDay = daysOfTheMonth.nth(index).locator(SCOPE_A);
            var href = linkOfEachDay.getAttribute(HREF);
            var dayNumber = linkOfEachDay.innerText();
            nextLinks.add(TheatreArtBgConstants.BASE_URL + href);
            LOG.info(String.format("%s: %s", dayNumber, href));
        }

        return nextLinks;
    }

    @Override
    public void scrape(ImmutableTheatreArtBgCalendar calendar, Page page) {
        var maybeMetadata = extractPlayData(page);
        if (maybeMetadata.isEmpty()) {
            return;
        }

        var playMetadataRecords = getRecords(maybeMetadata.get(), calendar);
        var mergeResult = theatrePlayDao.merge(playMetadataRecords);
        runPostMergePlayScraping(mergeResult);
        LOG.info(String.format("Finished scraping: %s", page.url()));
    }

    private List<TheatrePlayRecord> getRecords(
            ImmutableTheatreArtBgExtractedDayMetadata metadata, ImmutableTheatreArtBgCalendar calendar) {
        return metadata.getPlaysMetadata()
                .stream()
                .map(play -> {
                    var localDateTime = DateUtils.toOffsetDateTime(calendar.getYear(), calendar.getMonth(),
                                                         metadata.getDay(), play.getHour(), play.getMinute());
                    return new TheatrePlayRecord()
                            .setTitle(play.getTitle())
                            .setUrl(play.getUrl())
                            .setTheatre(play.getTheatre())
                            .setDate(localDateTime);
                }).toList();
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
                    if (playsToVisitSet.add(Pair.of(object.getRecord().getUrl(), object.getRecord().getDate()))) {
                        queue.add(ImmutableTheatreArtQueuePayload.builder()
                                          .url(object.getRecord().getUrl())
                                          .object(ImmutableTheatreArtBgPlayObject.builder().build())
                                          .build());
                    }
                }
            }
        });
    }

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
