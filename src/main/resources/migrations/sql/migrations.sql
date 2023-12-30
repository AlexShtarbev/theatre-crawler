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
ADD FOREIGN KEY (url, date) REFERENCES theatre.theatre_play (url, date)
ON DELETE CASCADE ON UPDATE CASCADE;

--changeset alex.shtarbev:5
create table if not exists theatre.theatre_play_details (
     url                          text NOT NULL,
     description                  text NULL,
     crew                         text NULL,
     rating                       text NULL,
     PRIMARY KEY (url)
);

--changeset saleksandar:6
CREATE INDEX theatre_play_url  ON theatre.theatre_play(url);

--changeset saleksandar:7
CREATE OR REPLACE FUNCTION pos_org_rel_refresh()
  RETURNS trigger AS
$func$
-- DECLARE
--    r int;  -- not used in function body
BEGIN
-- IF TG_OP='UPDATE' THEN  -- redundant while func is only used in AFTER UPDATE trigger
   DELETE FROM theatre.theatre_play_details
   USING  theatre.theatre_play
   WHERE  theatre.theatre_play.url = NEW.url
   AND    theatre.theatre_play.url = s.theatre.theatre_play_details.url;
-- END IF;

RETURN NEW;  --  and don't place this inside the IF block either way

END
$func$  LANGUAGE plpgsql;  -- don't quote the language name

--changeset saleksandar:8
CREATE TRIGGER theatre_play_url_trigger
AFTER DELETE
ON theatre.theatre_play
FOR EACH ROW
EXECUTE PROCEDURE pos_org_rel_refresh();