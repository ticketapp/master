# --- !Ups
-------------------------------------------------------- users ---------------------------------------------------------
INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email0', 'avatarUrl');

INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('b4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email00', 'avatarUrl');

INSERT INTO users(userID, email) VALUES ('077f3ea6-2272-4457-a47e-9e9111108e44', 'user@facebook.com');

-------------------------------------------------------- artists -------------------------------------------------------
INSERT INTO artists(name, facebookurl) VALUES('name', 'facebookUrl0');

INSERT INTO artists(name, facebookurl) VALUES('name0', 'facebookUrl00');
INSERT INTO artists(facebookid, name, facebookurl) VALUES('facebookIdTestTrack', 'artistTest', 'artistFacebookUrlTestPlaylistModel');

-------------------------------------------------------- tracks --------------------------------------------------------
INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('24894e56-08d1-4c1f-b3e4-466c069d15ed', 'title00', 'url000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');

-------------------------------------------------------- genres --------------------------------------------------------
INSERT INTO genres(name, icon) VALUES('genreTest0', 'a');
INSERT INTO genres(name, icon) VALUES('genreTest00', 'a');

-------------------------------------------------------- artistsGenres -------------------------------------------------
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest0'), 1);

INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest00'), 1);

-------------------------------------------------------- events --------------------------------------------------------
INSERT INTO events(ispublic, isactive, name, starttime) VALUES(true, true, 'name0', current_timestamp);
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime) VALUES(true, true, 'notPassedEvent', timestamp '2050-08-24 14:00:00');

-------------------------------------------------------- organizers ----------------------------------------------------
INSERT INTO organizers(name) VALUES('name0');
INSERT INTO organizers(name, facebookid) VALUES('name1', 'facebookId');

-------------------------------------------------------- eventsOrganizers ----------------------------------------------
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'notPassedEvent'), (SELECT organizerid FROM organizers WHERE name = 'name0'));

-------------------------------------------------------- eventsGenres --------------------------------------------------
INSERT INTO eventsgenres(eventid, genreid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT genreid FROM genres WHERE name = 'genreTest0'));

-------------------------------------------------------- eventsArtists -------------------------------------------------
INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));

  INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));

INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl00'));

-------------------------------------------------------- eventsFollowed ------------------------------------------------
INSERT INTO eventsfollowed(eventid, userid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT userid FROM users WHERE email = 'user@facebook.com'));


-- INSERT INTO playliststracks()
--
--
--
-- playlistId              BIGINT REFERENCES playlists (playlistId),
-- trackId                 UUID REFERENCES tracks (trackId),
-- trackRank               DOUBLE PRECISION NOT NULL
-- );
