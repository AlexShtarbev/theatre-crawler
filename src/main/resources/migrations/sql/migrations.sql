--liquibase formatted sql
--changeset alex.shtarbev:1
create schema theatre;

--changeset alex.shtarbev:2
create table if not exists theatre.theatre_play (
    url                          text NOT NULL,
    theatre                      text NOT NULL,
    title                        text NOT NULL,
    origin                       text NOT NULL,
    date                         timestamptz NOT NULL,
    last_updated                 timestamptz DEFAULT (now()),
    tickets_url                  text NULL,
    PRIMARY KEY (url, date)
);

--changeset saleksandar:3
create index theatre_play_url_idx on theatre.theatre_play(url);

--changeset saleksandar:4
create index theatre_play_origin_and_date_idx on theatre.theatre_play(origin, date);

--changeset alex.shtarbev:5
create table if not exists theatre.theatre_play_details (
     url                          text NOT NULL,
     description                  text NULL,
     crew                         text NULL,
     rating                       text NULL,
     origin                       text NULL,
     last_updated                 timestamptz DEFAULT (now()),
     PRIMARY KEY (url)
);

--changeset alex.shtarbev:6
create table if not exists theatre.google_calendar_events (
    url                          text NOT NULL,
    date                         timestamptz NOT NULL,
    eventId                      text NOT NULL,
    PRIMARY KEY (url, date)
);

--changeset alex.shtarbev:7
ALTER TABLE theatre.google_calendar_events
ADD FOREIGN KEY (url, date) REFERENCES theatre.theatre_play(url, date)
ON DELETE CASCADE ON UPDATE CASCADE;