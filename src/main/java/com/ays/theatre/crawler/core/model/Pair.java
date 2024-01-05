/*
 * Copyright (c) 2022 VMware, Inc. All rights reserved. VMware Confidential
 */

package com.ays.theatre.crawler.core.model;

import org.immutables.value.Value;

@Value.Immutable
public interface Pair<T, P> {

    T getFirst();
    P getSecond();
}
