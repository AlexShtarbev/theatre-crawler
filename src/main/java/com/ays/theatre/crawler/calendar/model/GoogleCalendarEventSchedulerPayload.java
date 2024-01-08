/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.calendar.model;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface GoogleCalendarEventSchedulerPayload {
    String getUrl();
    String getTitle();
    String getTheatre();
    String getCrew();
    String getDescription();
    String getRating();
    OffsetDateTime getStartTime();
    OffsetDateTime getLastUpdated();

    @Value.Default
    default Optional<String> getTheatreArtBgTicket() {
        return Optional.empty();
    }
}
