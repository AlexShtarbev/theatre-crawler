--liquibase formatted sql

--changeset alex.shtarbev:1
create schema theatre_crawler;

--changeset alex.shtarbev:2
create table if not exists theatre_crawler.theatre_play (
    title                        text DEFAULT gen_random_uuid() PRIMARY KEY,
    url                          text NOT NULL,
    date                         timestamptz NOT NULL,
    last_updated                 timestamptz DEFAULT (now())
);