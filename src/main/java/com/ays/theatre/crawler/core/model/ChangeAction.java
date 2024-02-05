package com.ays.theatre.crawler.core.model;

public enum ChangeAction {
    NONE, // no changes to the record
    NEW, // an existing record was updated
    ERROR // an error occurred while performing the change
}
