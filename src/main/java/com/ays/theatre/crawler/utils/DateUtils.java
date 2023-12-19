package com.ays.theatre.crawler.utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DateUtils {
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

}
