# --- !Ups

BEGIN
INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email0', 'avatarUrl');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO artists(name, facebookurl) VALUES ('name', 'facebookUrl0');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO artists(name, facebookurl) VALUES ('name0', 'facebookUrl00');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;

BEGIN
INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;

BEGIN
INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('24894e56-08d1-4c1f-b3e4-466c069d15ed', 'title00', 'url000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO genres (name, icon) VALUES ('genreTest0', 'a');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO genres (name, icon) VALUES ('genreTest00', 'a');
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;


BEGIN
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest0'), 1);
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;

BEGIN
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'), (SELECT genreid FROM genres WHERE name = 'genreTest00'), 1);
EXCEPTION WHEN unique_violation THEN
-- Ignore duplicate inserts.
END;

-- INSERT INTO playliststracks()
--
--
--
-- playlistId              BIGINT REFERENCES playlists (playlistId),
-- trackId                 UUID REFERENCES tracks (trackId),
-- trackRank               DOUBLE PRECISION NOT NULL
-- );