package com.ays.theatre.crawler.core.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.google.api.services.calendar.model.EventDateTime;

public class DateUtils {

    private static final DateTimeFormatter RFC_3339_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX")
            .withResolverStyle(ResolverStyle.LENIENT);

    public static Map<String, Integer> BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP = new HashMap<>() {{
        put("януари", Calendar.JANUARY + 1);
        put("февруари", Calendar.FEBRUARY + 1);
        put("март", Calendar.MARCH + 1);
        put("април", Calendar.APRIL + 1);
        put("май", Calendar.MAY + 1);
        put("юни", Calendar.JUNE + 1);
        put("юли", Calendar.JULY + 1);
        put("август", Calendar.AUGUST + 1);
        put("сетптември", Calendar.SEPTEMBER + 1);
        put("октомври", Calendar.OCTOBER + 1);
        put("ноември", Calendar.NOVEMBER + 1);
        put("декември", Calendar.DECEMBER + 1);
    }};

    public static Calendar getDateWithoutTimeUsingCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public static OffsetDateTime toOffsetDateTime(int year, int month, int day, int hour, int minute) {
        return OffsetDateTime.ofInstant(
                LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC),
                ZoneId.of("UTC"));
    }

    public static OffsetDateTime toOffsetDateTime(EventDateTime eventDateTime) {
        var rfcDateTime = eventDateTime.getDateTime().toStringRfc3339();
        var localDateTime = LocalDateTime.parse(rfcDateTime, RFC_3339_FORMATTER);
        return OffsetDateTime.of(LocalDateTime.parse(rfcDateTime, RFC_3339_FORMATTER), ZoneOffset.UTC);
    }

}
