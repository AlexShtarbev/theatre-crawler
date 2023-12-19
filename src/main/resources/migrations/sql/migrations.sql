--liquibase formatted sql

--changeset alex.shtarbev:1
create table if not exists theatre_play (
    title                        text NOT NULL,
    url                          text NOT NULL,
    theatre                      text NOT NULL,
    date                         timestamptz NOT NULL,
    last_updated                 timestamptz DEFAULT (now()),
    PRIMARY KEY (url, date)
);