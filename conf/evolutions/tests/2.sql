# --- !Ups
-------------------------------------------------------- users ---------------------------------------------------------
INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email0', 'avatarUrl');

INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('b4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email00', 'avatarUrl');

INSERT INTO users(userID, email) VALUES ('077f3ea6-2272-4457-a47e-9e9111108e44', 'user@facebook.com');

-------------------------------------------------------- artists -------------------------------------------------------
INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
INSERT INTO artists(artistid, name, facebookurl) VALUES('200', 'name0', 'facebookUrl00');
INSERT INTO artists(artistid, facebookid, name, facebookurl) VALUES('300', 'facebookIdTestTrack', 'artistTest', 'artistFacebookUrlTestPlaylistModel');
INSERT INTO artists(facebookid, name, facebookurl) VALUES('withoutEventRelation', 'withoutEventRelation', 'withoutEventRelation');
INSERT INTO artists(facebookid, name, facebookurl) VALUES('testFindIdByFacebookId', 'name00', 'testFindIdByFacebookId');

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
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(true, true, 'inProgressEvent', timestamp '2012-08-24 14:00:00', timestamp '2042-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime) VALUES(true, true, 'eventPassedWithoutEndTime', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(true, true, 'notPassedEvent', timestamp '2040-08-24 14:00:00', '0101000000654D87A9886F4840D146640E38D10240');
INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint) VALUES(100, true, true, 'notPassedEvent2', timestamp '2050-08-24 14:00:00', '01010000008906CEBE97E346405187156EF9581340');

-------------------------------------------------------- organizers ----------------------------------------------------
INSERT INTO organizers(name) VALUES('name0');
INSERT INTO organizers(organizerid, name, facebookid, geographicpoint) VALUES(100, 'name1', 'facebookId', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO organizers(organizerid, name, facebookid, geographicpoint) VALUES(300, 'name2', 'facebookId1', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- places --------------------------------------------------------
INSERT INTO places(placeid, name, facebookid, geographicpoint) VALUES(100, 'Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO places(placeid, name, facebookid, geographicpoint) VALUES(300, 'Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');
INSERT INTO places(placeid, name, facebookid) VALUES(400, 'testId4BecauseThereIsTRANSBORDEUR', 'facebookIdTestFollowController');
INSERT INTO places(placeid, name, facebookid) VALUES(500, 'testId5', 'facebookIdTestFollowController2');

-------------------------------------------------------- addresses -----------------------------------------------------
INSERT INTO addresses(city) VALUES('lyon');

-------------------------------------------------------- eventsPlaces --------------------------------------------------
INSERT INTO eventsplaces(eventid, placeid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT placeid FROM places WHERE name = 'Test'));
INSERT INTO eventsplaces(eventid, placeid) VALUES
  ((SELECT eventId FROM events WHERE name = 'notPassedEvent'), (SELECT placeid FROM places WHERE name = 'Test'));
INSERT INTO eventsplaces(eventid, placeid) VALUES
  ((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT placeid FROM places WHERE name = 'Test'));

  -------------------------------------------------------- eventsOrganizers --------------------------------------------
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
INSERT INTO eventsorganizers(eventid, organizerid) VALUES
  ((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT organizerid FROM organizers WHERE name = 'name0'));

-------------------------------------------------------- eventsGenres --------------------------------------------------
INSERT INTO eventsgenres(eventid, genreid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT genreid FROM genres WHERE name = 'genreTest0'));

  -------------------------------------------------------- eventsAddresses ---------------------------------------------
INSERT INTO eventsaddresses(eventid, addressid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT addressid FROM addresses WHERE city = 'lyon'));

-------------------------------------------------------- eventsArtists -------------------------------------------------
INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));

  INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));

INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl00'));

-------------------------------------------------------- eventsFollowed ------------------------------------------------
INSERT INTO eventsfollowed(eventid, userid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- organizersFollowed --------------------------------------------
INSERT INTO organizersfollowed(organizerid, userid) VALUES
  (1, '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- placesFollowed ------------------------------------------------
INSERT INTO placesfollowed(placeid, userid) VALUES (400, '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- frenchCities --------------------------------------------------
INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- playlistsTracks -----------------------------------------------
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(1, '02894e56-08d1-4c1f-b3e4-466c069d15ed', 1);
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(1, '13894e56-08d1-4c1f-b3e4-466c069d15ed', 2);

-------------------------------------------------------- tracksGenres --------------------------------------------------
INSERT INTO tracksgenres(genreid, trackid, weight) VALUES(1, '13894e56-08d1-4c1f-b3e4-466c069d15ed', 1);

-------------------------------------------------------- tracksFollowed -------------------------------------------
INSERT INTO tracksfollowed(userId, trackId) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '02894e56-08d1-4c1f-b3e4-466c069d15ed');

-------------------------------------------------------- tracksRemoved -------------------------------------------
INSERT INTO tracksrating(userId, trackId, reason) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '13894e56-08d1-4c1f-b3e4-466c069d15ed', 'a');