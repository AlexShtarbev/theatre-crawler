
package com.ays.theatre.crawler.theatreartbg.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.ays.theatre.crawler.theatreartbg.TheatreArtBgConstants;
import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtBgCalendar;
import com.ays.theatre.crawler.utils.DateUtils;
import com.microsoft.playwright.Page;

import jakarta.inject.Singleton;

@Singleton
public class TheatreArtBgCrawlerService {
    private static final Logger LOG = Logger.getLogger(TheatreArtBgCrawlerService.class);
    private static final String HREF = "href";
    private static final String SCOPE_A = ":scope a";

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
}
