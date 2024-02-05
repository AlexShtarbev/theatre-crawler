/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.core.model;

import org.immutables.value.Value;

import com.ays.theatre.crawler.tables.records.TheatrePlayDetailsRecord;
import com.ays.theatre.crawler.tables.records.TheatrePlayRecord;

@Value.Immutable
public interface ResycRecord {

    TheatrePlayRecord getPlayRecord();
    TheatrePlayDetailsRecord getDetailsRecord();
}
