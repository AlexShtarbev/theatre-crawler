/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.theatreartbg.model;

import org.immutables.value.Value;

@Value.Immutable
public interface TheatreArtQueuePayload {

    String getUrl();
}
