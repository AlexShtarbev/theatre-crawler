package com.ays.theatre.crawler.utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DateUtils {
    public static Map<String, Integer> BULGARIAN_MONTH_TO_CALENDAR_MONTH_MAP = new HashMap() {{
        put("януари", Calendar.JANUARY);
        put("февруари", Calendar.FEBRUARY);
        put("март", Calendar.MARCH);
        put("април", Calendar.APRIL);
        put("май", Calendar.MAY);
        put("юни", Calendar.JUNE);
        put("юли", Calendar.JULY);
        put("август", Calendar.AUGUST);
        put("сетптември", Calendar.SEPTEMBER);
        put("октомври", Calendar.OCTOBER);
        put("ноември", Calendar.NOVEMBER);
        put("декември", Calendar.DECEMBER);
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
