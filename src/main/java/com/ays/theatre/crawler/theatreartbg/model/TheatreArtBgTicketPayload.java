/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.model;

import java.time.OffsetDateTime;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtBgTicketPayload {
    String getUrl();
    OffsetDateTime getDate();
    String getTicketUrl();

}
