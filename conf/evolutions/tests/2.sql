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
INSERT INTO artists(facebookid, name, facebookurl) VALUES('withoutEventRelation', 'withoutEventRelation', 'withoutEventRelation');

-------------------------------------------------------- tracks --------------------------------------------------------
INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('24894e56-08d1-4c1f-b3e4-466c069d15ed', 'title00', 'url000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('35894e56-08d1-4c1f-b3e4-466c069d15ed', 'title000', 'url0000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');

-------------------------------------------------------- playlists -----------------------------------------------------
INSERT INTO playlists(userId, name) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', 'playlist0');

-------------------------------------------------------- genres --------------------------------------------------------
INSERT INTO genres(name, icon) VALUES('genreTest0', 'a');
INSERT INTO genres(name, icon) VALUES('genreTest00', 'a');

-------------------------------------------------------- artistsGenres -------------------------------------------------
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest0'), 1);

INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest00'), 1);

-------------------------------------------------------- events --------------------------------------------------------
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(true, true, 'name0', current_timestamp, '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(true, true, 'notPassedEvent', timestamp '2050-08-24 14:00:00', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- organizers ----------------------------------------------------
INSERT INTO organizers(name) VALUES('name0');
INSERT INTO organizers(name, facebookid, geographicpoint) VALUES('name1', 'facebookId', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO organizers(name, facebookid, geographicpoint) VALUES('name2', 'facebookId1', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- places ----------------------------------------------------
INSERT INTO places(name, facebookid, geographicpoint) VALUES('Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO places(name, facebookid, geographicpoint) VALUES('Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');

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

-------------------------------------------------------- frenchCities --------------------------------------------------
INSERT INTO frenchcities(city, geographicpoint) VALUES('Lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- playlistsTracks -----------------------------------------------
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(1, '02894e56-08d1-4c1f-b3e4-466c069d15ed', 1);
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(1, '13894e56-08d1-4c1f-b3e4-466c069d15ed', 2);

-------------------------------------------------------- tracksGenres --------------------------------------------------
INSERT INTO tracksgenres(genreid, trackid, weight) VALUES(1, '13894e56-08d1-4c1f-b3e4-466c069d15ed', 1);

-------------------------------------------------------- tracksFollowed -------------------------------------------
INSERT INTO tracksfollowed(userId, trackId) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '02894e56-08d1-4c1f-b3e4-466c069d15ed');