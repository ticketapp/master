# --- !Ups
INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon',
'garnier', 'fullname', 'email0', 'avatarUrl');

INSERT INTO artists(name, facebookurl) VALUES ('name', 'facebookUrl0');

INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname) VALUES
  ('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

-- INSERT INTO playliststracks()
--
--
--
-- playlistId              BIGINT REFERENCES playlists (playlistId),
-- trackId                 UUID REFERENCES tracks (trackId),
-- trackRank               DOUBLE PRECISION NOT NULL
-- );