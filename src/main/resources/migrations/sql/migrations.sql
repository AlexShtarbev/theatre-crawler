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
     PRIMARY KEY (url)
);