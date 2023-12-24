--liquibase formatted sql
--changeset alex.shtarbev:1
create schema theatre;

--changeset alex.shtarbev:2
create table if not exists theatre.theatre_play (
    url                          text NOT NULL,
    date                         timestamptz NOT NULL,
    theatre                      text NOT NULL,
    title                        varchar NOT NULL,
    PRIMARY KEY (url, date)
);

--changeset alex.shtarbev:3
create table if not exists theatre.theatre_play_metadata (
    url                          text NOT NULL,
    date                         timestamptz NOT NULL,
    last_updated                 timestamptz DEFAULT (now()),
    PRIMARY KEY (url, date)
);

--changeset saleksandar:4
ALTER TABLE theatre.theatre_play_metadata
ADD FOREIGN KEY (url, date) REFERENCES theatre.theatre_play_metadata (url, date)
ON DELETE CASCADE ON UPDATE CASCADE;