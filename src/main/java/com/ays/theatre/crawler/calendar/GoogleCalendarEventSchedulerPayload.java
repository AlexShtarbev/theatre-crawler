/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.calendar;

import java.time.OffsetDateTime;

import org.immutables.value.Value;

import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;

@Value.Immutable
public interface GoogleCalendarEventSchedulerPayload {
    String getUrl();
    String getTitle();
    String getTheatre();
    String getCrew();
    String getDescription();
    String getRating();
    OffsetDateTime getStartTime();
}
