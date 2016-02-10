# --- !Ups
CREATE TABLE infos (
  infoId                    SERIAL PRIMARY KEY,
  displayIfConnected        BOOLEAN NOT NULL DEFAULT TRUE,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  animationContent          VARCHAR,
  animationStyle            VARCHAR
);

CREATE TABLE frenchCities (
  cityId                    SERIAL PRIMARY KEY,
  city                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY NOT NULL
);
CREATE INDEX frenchCityGeographicPoints ON frenchCities USING GIST (geographicPoint);
CREATE INDEX frenchCityNames ON frenchCities (city);


CREATE TABLE addresses (
  addressId                 SERIAL PRIMARY KEY,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  city                      VARCHAR(127),
  zip                       VARCHAR(15),
  street                    VARCHAR
);
CREATE INDEX geographicPointAddresses ON addresses USING GIST (geographicPoint);
CREATE UNIQUE INDEX addressesIndex ON addresses (city, zip, street);


CREATE TABLE orders ( --account701
  orderId                   SERIAL PRIMARY KEY,
  totalPrice                INT NOT NULL
);


CREATE TABLE artists (
  artistId                  SERIAL PRIMARY KEY,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  facebookId                VARCHAR(63),
  name                      VARCHAR(255) NOT NULL,
  imagePath                 VARCHAR,
  description               VARCHAR,
  facebookUrl               VARCHAR(255) NOT NULL,
  websites                  VARCHAR,
  hasTracks                 BOOLEAN DEFAULT FALSE,
  likes                     INTEGER,
  country                   VARCHAR,
  UNIQUE(facebookId),
  UNIQUE(facebookUrl)
);


CREATE TABLE organizers (
  organizerId             SERIAL PRIMARY KEY,
  facebookId              VARCHAR(63),
  name                    VARCHAR(255) NOT NULL,
  description             VARCHAR,
  addressId               BIGINT REFERENCES addresses(addressId) ON DELETE CASCADE,
  phone                   VARCHAR(255),
  publicTransit           VARCHAR,
  websites                VARCHAR,
  verified                BOOLEAN DEFAULT FALSE NOT NULL,
  imagePath               VARCHAR,
  geographicPoint         GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  placeId                 BIGINT,
  UNIQUE(facebookId)
);


CREATE TABLE genres (
  genreId                 SERIAL PRIMARY KEY,
  name                    VARCHAR NOT NULL,
  icon                    CHAR NOT NULL,
  UNIQUE(name)
);


CREATE TABLE tracks (
  trackId                 UUID PRIMARY KEY NOT NULL,
  title                   VARCHAR(255) NOT NULL,
  url                     VARCHAR NOT NULL,
  platform                CHAR NOT NULL,
  thumbnailUrl            VARCHAR NOT NULL,
  artistFacebookUrl       VARCHAR(255) REFERENCES artists(facebookUrl) ON DELETE CASCADE NOT NULL,
  artistName              VARCHAR(255) NOT NULL,
  redirectUrl             VARCHAR(255),
  confidence              DOUBLE PRECISION NOT NULL DEFAULT 0,
  ratingUp                INT NOT NULL DEFAULT 0,
  ratingDown              INT NOT NULL DEFAULT 0,
  UNIQUE(url)
);
CREATE UNIQUE INDEX artistNameAndTitle ON tracks(title, artistFacebookUrl);
CREATE INDEX artistFacebookUrl ON tracks(artistFacebookUrl);


create table users (
  userId                    UUID PRIMARY KEY,
  firstName                 VARCHAR,
  lastName                  VARCHAR,
  fullName                  VARCHAR,
  email                     VARCHAR UNIQUE,
  avatarURL                 VARCHAR
);


CREATE TABLE logininfo (
  id                       SERIAL PRIMARY KEY,
  providerId               VARCHAR NOT NULL,
  providerKey              VARCHAR NOT NULL
);


CREATE TABLE userlogininfo (
  userId                   UUID PRIMARY KEY,
  loginInfoId              BIGINT NOT NULL
);


CREATE TABLE passwordinfo (
  hasher                  VARCHAR NOT NULL,
  password                VARCHAR NOT NULL,
  salt                    VARCHAR,
  loginInfoId             BIGINT NOT NULL
);


CREATE TABLE oauth1info (
  id                      SERIAL PRIMARY KEY,
  token                   VARCHAR NOT NULL,
  secret                  VARCHAR NOT NULL,
  loginInfoId             BIGINT NOT NULL
);


CREATE TABLE oauth2info (
  id                      SERIAL PRIMARY KEY,
  accesstoken             VARCHAR NOT NULL,
  tokentype               VARCHAR,
  expiresin               INTEGER,
  refreshtoken            VARCHAR,
  logininfoid             BIGINT NOT NULL
);


CREATE TABLE openidinfo (
  id                      VARCHAR NOT NULL PRIMARY KEY,
  logininfoid             BIGINT NOT NULL
);


CREATE TABLE openidattributes (
  id                      VARCHAR NOT NULL,
  key                     VARCHAR NOT NULL,
  value                   VARCHAR NOT NULL
);


CREATE TABLE receivedMails (
  id                        SERIAL PRIMARY KEY,
  subject                   VARCHAR NOT NULL,
  message                   VARCHAR NOT NULL,
  read                      BOOLEAN NOT NULL DEFAULT FALSE,
  userId                    UUID REFERENCES users(userId) ON DELETE CASCADE
);


CREATE TABLE events (
  eventId                   SERIAL PRIMARY KEY,
  facebookId                VARCHAR(63),
  isPublic                  BOOLEAN NOT NULL,
  isActive                  BOOLEAN NOT NULL,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  description               VARCHAR,
  startTime                 TIMESTAMP NOT NULL,
  endTime                   TIMESTAMP,
  imagePath                 VARCHAR,
  ageRestriction            SMALLINT NOT NULL DEFAULT 16,
  tariffRange               VARCHAR(15),
  ticketSellers             VARCHAR,
  UNIQUE(facebookId)
);
CREATE INDEX eventGeographicPoint ON events USING GIST (geographicPoint);


CREATE TABLE places (
  placeId                   SERIAL PRIMARY KEY,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  addressId                 BIGINT REFERENCES addresses(addressId) ON DELETE CASCADE,
  facebookId                VARCHAR(63),
  description               VARCHAR,
  webSites                  VARCHAR,
  facebookMiniature         VARCHAR,
  capacity                  INT,
  openingHours              VARCHAR(255),
  imagePath                 VARCHAR,
  linkedOrganizerId         BIGINT,
  UNIQUE(facebookId)
);
CREATE INDEX placeGeographicPoint ON places USING GIST (geographicPoint);


CREATE TABLE images (
  imageId                     SERIAL PRIMARY KEY,
  path                        VARCHAR NOT NULL,
  category                    VARCHAR(31),
  organizerId                 BIGINT REFERENCES organizers(organizerId) ON DELETE CASCADE,
  infoId                      BIGINT REFERENCES infos(infoId) ON DELETE CASCADE,
  trackId                     UUID REFERENCES tracks(trackId) ON DELETE CASCADE,
  UNIQUE(path)
);


CREATE TABLE tariffs (
  tariffId                  SERIAL PRIMARY KEY,
  denomination              VARCHAR(255) DEFAULT 'Basique' NOT NULL,
  price                     NUMERIC NOT NULL,
  startTime                 TIMESTAMP NOT NULL,
  endTime                   TIMESTAMP NOT NULL,
  eventId                   BIGINT REFERENCES events(eventId) ON DELETE CASCADE
);




CREATE TABLE tickets (
  ticketId                  SERIAL PRIMARY KEY,
  qrCode                    VARCHAR(255) UNIQUE NOT NULL,
  eventId                   INT REFERENCES events(eventId) NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId) NOT NULL
);
CREATE INDEX ticketQrCode ON tickets (qrCode);

CREATE TABLE ticketStatuses (
  id                        SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  status                    CHAR NOT NULL,
  date                      TIMESTAMP NOT NULL
);

CREATE TABLE blockedTickets (
  id                        SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  expirationDate            TIMESTAMP NOT NULL,
  userId                    UUID REFERENCES users(userID) NOT NULL
);
CREATE INDEX blockedTicketDate ON blockedTickets (expirationDate);

CREATE TABLE boughtTicketBills (
  billId                    SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  date                      TIMESTAMP NOT NULL,
  amount                    NUMERIC NOT NULL
);

CREATE TABLE soldTicketBills (
  billId                    SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  date                      TIMESTAMP NOT NULL,
  amount                    NUMERIC NOT NULL
);

CREATE TABLE pendingTickets (
  pendingTicketId             SERIAL PRIMARY KEY,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId)  NOT NULL,
  date                      TIMESTAMP NOT NULL,
  amount                    NUMERIC NOT NULL,
  qrCode                    VARCHAR UNIQUE NOT NULL,
  isValidated               BOOLEAN
);
CREATE INDEX pendingTicketQrCode ON pendingTickets (qrCode);


CREATE TABLE salableEvents (
  eventId                   INT PRIMARY KEY REFERENCES events(eventId) NOT NULL
);

CREATE TABLE issues (
  issueId                   SERIAL PRIMARY KEY,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId) ON DELETE CASCADE,
  fixed                     BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE issuesComments (
  commentId                 SERIAL PRIMARY KEY,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId) ON DELETE CASCADE,
  issueId                   BIGINT REFERENCES issues(issueId) ON DELETE CASCADE
);

CREATE TABLE usersTools (
  tableId                   SERIAL PRIMARY KEY,
  tools                     VARCHAR(255) NOT NULL,
  userId                    UUID REFERENCES users(userId) ON DELETE CASCADE
);

---############################## ACCOUNTING ###################################

---Vente de produits finis = account701 = table order

CREATE TABLE clients (
  clientId                SERIAL PRIMARY KEY,
  name                    VARCHAR(255),
  contactName             VARCHAR(255),
  socialDenomination      VARCHAR(255),
  addressID               BIGINT REFERENCES addresses(addressID) ON DELETE CASCADE,
  email                   VARCHAR(255),
  UNIQUE(email)
);

CREATE TABLE bills (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  billingDate             TIMESTAMP,
  amountDue               INT,
  endSellingTime          TIMESTAMP
  --majorations impayés
);


--Produit activités annexes = compte 708
CREATE TABLE account708 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP DEFAULT  current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  clientId                BIGINT NOT NULL REFERENCES clients(clientId),
  orderId                 BIGINT NOT NULL REFERENCES orders(orderId)
);

--Quand le client a payé sa facture
CREATE TABLE account411 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT NOT NULL REFERENCES clients(clientId),
  paymentDate             TIMESTAMP NOT NULL,
  amount                  INT NOT NULL,
  paymentMean             VARCHAR(255) NOT NULL
);

--Ce qui est dû par le client
CREATE TABLE account413 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  date                    TIMESTAMP DEFAULT current_timestamp NOT NULL,
  amount                  INT NOT NULL,
  debit                   Boolean NOT NULL
);

--Accomptes payés par les clients
CREATE TABLE account4191 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  billId                  BIGINT REFERENCES bills(id),
  paymentDate             TIMESTAMP NOT NULL,
  amount                  INT NOT NULL,
  paymentMean             VARCHAR(255) NOT NULL
);


--associés, dividendes à payer
CREATE TABLE account457 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP DEFAULT  current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  paymentReference        INT NOT NULL,
  assemblyName            VARCHAR(255) NOT NULL
);

--impôts et taxes
CREATE TABLE account63 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId),
  account708Id            BIGINT REFERENCES account708(id)
);

--diverses charges à payer
CREATE TABLE account4686 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255),
  amount                  NUMERIC NOT NULL,
  debit                   BOOLEAN NOT NULL,
  account63Id             BIGINT REFERENCES account63(id)
);


--achats
CREATE TABLE account60 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  paymentReference        INT,
  orderId                 BIGINT REFERENCES orders(orderId)
);

--Fournisseurs à payer
CREATE TABLE account403 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP DEFAULT current_timestamp NOT NULL,
  amount                  NUMERIC NOT NULL,
  debit                   BOOLEAN NOT NULL, --#sinon debit
  userId                  UUID REFERENCES users(userId),
  account60Id             BIGINT REFERENCES account60(id)
);

--frais postaux et telecoms
CREATE TABLE account626 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  paymentReference        INT NOT NULL
);

--services bancaires
CREATE TABLE account627 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId)
);


CREATE TABLE bank ( --account512
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) DEFAULT '666' NOT NULL,
  amount                  NUMERIC NOT NULL,
  debit                   Boolean NOT NULL,
  paymentReference        INT DEFAULT 0 NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId),
  account627Id            BIGINT REFERENCES account627(id)
);

--publicité publications et relations publiques
CREATE TABLE account623 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP DEFAULT  current_timestamp NOT NULL,
  amount                  NUMERIC NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  paymentReference        INT NOT NULL,
  billId                  BIGINT REFERENCES bills(id)
);

CREATE TABLE eventsFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  eventId                  BIGINT REFERENCES events(eventId) ON DELETE CASCADE
);
CREATE UNIQUE INDEX eventsFollowedIndex ON eventsFollowed (userId, eventId);

CREATE TABLE artistsFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  artistId                 INT REFERENCES artists(artistId) ON DELETE CASCADE
);
CREATE UNIQUE INDEX artistsFollowedIndex ON artistsFollowed (userId, artistId);


CREATE TABLE placesFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  placeId                  INT REFERENCES places(placeId) ON DELETE CASCADE NOT NULL
);
CREATE UNIQUE INDEX placesFollowedIndex ON placesFollowed (userId, placeId);

CREATE TABLE usersFollowed (
  tableId                 SERIAL PRIMARY KEY,
  userIdFollower          UUID REFERENCES users(userId),
  userIdFollowed          UUID REFERENCES users(userId)
);
CREATE UNIQUE INDEX usersFollowedIndex ON usersFollowed (userIdFollower, userIdFollowed);

CREATE TABLE organizersFollowed (
  tableId                 SERIAL PRIMARY KEY,
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE,
  organizerId             INT REFERENCES organizers(organizerId) ON DELETE CASCADE NOT NULL
);
CREATE UNIQUE INDEX organizersFollowedIndex ON organizersFollowed (userId, organizerId);

CREATE TABLE tracksFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  trackId                  UUID REFERENCES tracks(trackId) ON DELETE CASCADE
);
CREATE UNIQUE INDEX tracksFollowedIndex ON tracksFollowed (userId, trackId);

CREATE TABLE eventsPlaces (
  eventId                 BIGINT REFERENCES events (eventId) ON DELETE CASCADE,
  placeId                 INT REFERENCES places (placeId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, placeId)
);


CREATE TABLE eventsGenres (
  eventId                 INT REFERENCES events (eventId) ON DELETE CASCADE,
  genreId                 INT REFERENCES genres (genreId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, genreId)
);


CREATE TABLE eventsOrganizers (
  eventId                 INT REFERENCES events (eventId) ON DELETE CASCADE,
  organizerId             INT REFERENCES organizers(organizerId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, organizerId)
);


CREATE TABLE eventsAddresses (
  eventId                 INT REFERENCES events (eventId) ON DELETE CASCADE,
  addressId               INT REFERENCES addresses(addressId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, addressId)
);


CREATE TABLE usersOrganizers (
  tableId                 SERIAL PRIMARY KEY,
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE,
  organizerId             INT REFERENCES organizers(organizerId) ON DELETE CASCADE
);
CREATE UNIQUE INDEX usersOrganizersIndex ON usersOrganizers (userId, organizerId);


CREATE TABLE eventsArtists (
  eventId                 INT REFERENCES events (eventId) ON DELETE CASCADE,
  artistId                INT REFERENCES artists (artistId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, artistId)
);


CREATE TABLE tracksGenres (
  trackId                 UUID REFERENCES tracks (trackId) ON DELETE CASCADE NOT NULL,
  genreId                 INT REFERENCES genres (genreId) ON DELETE CASCADE NOT NULL,
  weight                  BIGINT NOT NULL,
  PRIMARY KEY (genreId)
);
CREATE UNIQUE INDEX tracksGenresIndex ON tracksGenres (trackId, genreId);


CREATE TABLE artistsGenres (
  artistId                INT REFERENCES artists (artistId) ON DELETE CASCADE,
  genreId                 INT REFERENCES genres (genreId) ON DELETE CASCADE,
  weight                  INT NOT NULL,
  PRIMARY KEY (artistId, genreId)
);


CREATE TABLE playlists (
  playlistId              SERIAL PRIMARY KEY,
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE NOT NULL,
  name                    VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX playlistsIndex ON playlists (playlistId, userId);


CREATE TABLE playlistsTracks (
  tableId                 SERIAL PRIMARY KEY,
  playlistId              BIGINT REFERENCES playlists (playlistId) ON DELETE CASCADE,
  trackId                 UUID REFERENCES tracks (trackId) ON DELETE CASCADE,
  trackRank               DOUBLE PRECISION NOT NULL
);
CREATE UNIQUE INDEX playlistsTracksIndex ON playlistsTracks (playlistId, trackId);


CREATE TABLE tracksRating (
  tableId                 SERIAL PRIMARY KEY,
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE NOT NULL,
  trackId                 UUID REFERENCES tracks (trackId) ON DELETE CASCADE NOT NULL,
  ratingUp                INT,
  ratingDown              INT,
  reason                  CHAR
);
CREATE UNIQUE INDEX tracksRatingIndex ON tracksRating (userId, trackId);


CREATE TABLE facebookAttendees (
  id                      SERIAL PRIMARY KEY,
  attendeeFacebookId      VARCHAR(255) NOT NULL UNIQUE,
  name                    VARCHAR(255) NOT NULL
);


CREATE TABLE facebookAttendeeEventRelations (
  attendeeFacebookId      VARCHAR REFERENCES facebookAttendees(attendeeFacebookId) NOT NULL,
  eventFacebookId         VARCHAR(63) REFERENCES events(facebookId) NOT NULL,
  attendeeStatus          CHAR,
  PRIMARY KEY (attendeeFacebookId, eventFacebookId)
);


-------------------------------------------------------- users ---------------------------------------------------------
INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES
  ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon', 'garnier', 'fullname', 'email0', 'avatarUrl');

INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL) VALUES
  ('b4aea509-1002-47d0-b55c-593c91cb32ae', 'simon', 'garnier', 'fullname', 'email00', 'avatarUrl');

INSERT INTO users(userID, email) VALUES ('077f3ea6-2272-4457-a47e-9e9111108e44', 'user@facebook.com');

-------------------------------------------------------- artists -------------------------------------------------------
INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
INSERT INTO artists(artistid, name, facebookurl) VALUES('200', 'name0', 'facebookUrl00');
INSERT INTO artists(artistid, facebookid, name, facebookurl)
  VALUES('300', 'facebookIdTestTrack', 'artistTest', 'artistFacebookUrlTestPlaylistModel');
INSERT INTO artists(facebookid, name, facebookurl)
  VALUES('withoutEventRelation', 'withoutEventRelation', 'withoutEventRelation');
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
INSERT INTO genres(name, icon) VALUES('genretest0', 'a');
INSERT INTO genres(name, icon) VALUES('genretest00', 'a');

-------------------------------------------------------- events --------------------------------------------------------
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(
  true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(
  true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(
  true, true, 'inProgressEvent', timestamp '2012-08-24 14:00:00', timestamp '2042-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime) VALUES(
  true, true, 'eventPassedWithoutEndTime', timestamp '2012-08-24 14:00:00');
INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(
  true, true, 'notPassedEvent', timestamp '2040-08-24 14:00:00', '0101000000654D87A9886F4840D146640E38D10240');
INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint) VALUES(
  100, true, true, 'notPassedEvent2', timestamp '2050-08-24 14:00:00', '01010000008906CEBE97E346405187156EF9581340');
INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime) VALUES(
  1000, 'facebookidattendeetest', true, true, 'notPassedEvent3', timestamp '2050-08-24 14:00:00');

-------------------------------------------------------- tariffs  ------------------------------------------------------
INSERT INTO tariffs(tariffId, denomination, price, startTime, endTime, eventId) VALUES
  (10000, 'test', 10, timestamp '2040-08-24 14:00:00', timestamp '2040-09-24 14:00:00', 100);

-------------------------------------------------------- tickets  ------------------------------------------------------
INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES
  (1000, 'savedTicket', 100, 10000);

INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES
  (1100, 'savedBlockedTicket', 100, 10000);

-------------------------------------------------------- ticketStatuses  ------------------------------------------------------
INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES
  (1000, 1000, 'a', timestamp '2015-09-22 14:00:00');

INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES
  (1100, 1000, 'b', timestamp '2015-09-24 14:00:00');

-------------------------------------------------------- pending tickets ----------------------------------------------------
INSERT INTO pendingTickets(pendingTicketId, userId, tariffId, date, amount, qrCode) VALUES
  (1000, 'a4aea509-1002-47d0-b55c-593c91cb32ae', 10000, timestamp '2015-09-24 14:00:00', 10, 'pendingTicket');



-------------------------------------------------------- bought tickets bills----------------------------------------------------
INSERT INTO boughtTicketBills(billId, ticketId, userId, date, amount) VALUES
  (1000, 1100, 'a4aea509-1002-47d0-b55c-593c91cb32ae', timestamp '2015-09-24 14:00:00', 10);

-------------------------------------------------------- sold tickets bills----------------------------------------------------
INSERT INTO soldTicketBills(billId, ticketId, userId, date, amount) VALUES
  (1000, 1100, 'a4aea509-1002-47d0-b55c-593c91cb32ae', timestamp '2015-09-24 14:00:00', 10);


-------------------------------------------------------- blocked tickets ----------------------------------------------------
INSERT INTO blockedTickets(id, ticketId, expirationDate, userId) VALUES
  (1000, 1100, timestamp '2055-09-24 14:00:00', 'a4aea509-1002-47d0-b55c-593c91cb32ae');


-------------------------------------------------------- salable events ----------------------------------------------------
INSERT INTO salableEvents(eventId) VALUES (100);


-------------------------------------------------------- organizers ----------------------------------------------------
INSERT INTO organizers(name) VALUES('name0');
INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
  VALUES(100, 'name1', 'facebookId', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
  VALUES(300, 'name2', 'facebookId1', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- places --------------------------------------------------------
INSERT INTO places(name, geographicPoint, facebookId)
  VALUES ('Le transbordeur', ST_GeomFromText('POINT(45.783808 4.860598)', 4326), '117030545096697');
INSERT INTO places(placeid, name, facebookid, geographicpoint)
  VALUES(100, 'Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');
INSERT INTO places(placeid, name, facebookid, geographicpoint)
  VALUES(300, 'Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');
INSERT INTO places(placeid, name, facebookid)
  VALUES(400, 'testId4BecauseThereIsTRANSBORDEUR', 'facebookIdTestFollowController');
INSERT INTO places(placeid, name, facebookid) VALUES(600, 'testId5', 'facebookId600');
INSERT INTO places(placeid, name, facebookid) VALUES(700, 'testId5', 'facebookId700');
INSERT INTO places(placeid, name, facebookid) VALUES(800, 'testId5', 'facebookId800');
INSERT INTO places(placeid, name, facebookid) VALUES(900, 'testId900', 'facebookId900');
INSERT INTO places(placeid, name, facebookid) VALUES(1000, 'testId5', 'facebookId1000');
INSERT INTO places(placeid, name, facebookid) VALUES(1100, 'testId5', 'facebookId1100');
INSERT INTO places(placeid, name, facebookid) VALUES(1200, 'testId5', 'facebookId1200');
INSERT INTO places(placeid, name, facebookid) VALUES(1300, 'testId5', 'facebookId1300');
INSERT INTO places(placeid, name, facebookid) VALUES(1400, 'testId5', 'facebookId1400');
INSERT INTO places(placeid, name, facebookid) VALUES(1500, 'testId5', 'facebookId1500');
INSERT INTO places(placeid, name, facebookid) VALUES(1600, 'testId5', 'facebookId1600');
INSERT INTO places(placeid, name, facebookid) VALUES(1700, 'testId5', 'facebookId1700');
INSERT INTO places(placeid, name, facebookid) VALUES(1800, 'testId5', 'facebookId1800');
INSERT INTO places(placeid, name, facebookid) VALUES(1900, 'testId5', 'facebookId1900');
INSERT INTO places(placeid, name, facebookid) VALUES(2000, 'testId5', 'facebookId2000');
INSERT INTO places(placeid, name, facebookid) VALUES(2100, 'testId5', 'facebookId2100');
INSERT INTO places(placeid, name, facebookid) VALUES(2200, 'testId5', 'facebookId2200');
INSERT INTO places(placeid, name, facebookid) VALUES(2300, 'testId5', 'facebookId2300');
INSERT INTO places(placeid, name, facebookid) VALUES(2400, 'testId5', 'facebookId2400');

-------------------------------------------------------- addresses -----------------------------------------------------
INSERT INTO addresses(city) VALUES('lyon');

-------------------------------------------------------- issues --------------------------------------------------------
INSERT INTO issues(issueid, title, content, userid, fixed)
VALUES(100, 'title', 'content', '077f3ea6-2272-4457-a47e-9e9111108e44', false);

-------------------------------------------------------- artistsGenres -------------------------------------------------
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
   (SELECT genreid FROM genres WHERE name = 'genretest0'), 1);
INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
  ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
   (SELECT genreid FROM genres WHERE name = 'genretest00'), 1);

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
  ((SELECT eventId FROM events WHERE name = 'notPassedEvent2'),
   (SELECT organizerid FROM organizers WHERE name = 'name0'));

-------------------------------------------------------- eventsGenres --------------------------------------------------
INSERT INTO eventsgenres(eventid, genreid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT genreid FROM genres WHERE name = 'genretest0'));

-------------------------------------------------------- eventsAddresses ---------------------------------------------
INSERT INTO eventsaddresses(eventid, addressid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), (SELECT addressid FROM addresses WHERE city = 'lyon'));

-------------------------------------------------------- eventsArtists -------------------------------------------------
INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'),
   (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));
INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'eventPassed'),
   (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));
INSERT INTO eventsartists(eventid, artistid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'),
   (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl00'));

-------------------------------------------------------- eventsFollowed ------------------------------------------------
INSERT INTO eventsfollowed(eventid, userid) VALUES
  ((SELECT eventId FROM events WHERE name = 'name0'), '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- organizersFollowed --------------------------------------------
INSERT INTO organizersfollowed(organizerid, userid) VALUES
  ((SELECT organizerId FROM organizers WHERE name = 'name0'), '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- placesFollowed ------------------------------------------------
INSERT INTO placesfollowed(placeid, userid) VALUES (400, '077f3ea6-2272-4457-a47e-9e9111108e44');

-------------------------------------------------------- frenchCities --------------------------------------------------
INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');

-------------------------------------------------------- playlistsTracks -----------------------------------------------
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(
  (SELECT playlistid FROM playlists WHERE name = 'playlist0'), '02894e56-08d1-4c1f-b3e4-466c069d15ed', 1);
INSERT INTO playliststracks(playlistId, trackid, trackrank) VALUES(
  (SELECT playlistid FROM playlists WHERE name = 'playlist0'), '13894e56-08d1-4c1f-b3e4-466c069d15ed', 2);

-------------------------------------------------------- tracksGenres --------------------------------------------------
INSERT INTO tracksgenres(genreid, trackid, weight) VALUES(
  (SELECT genreid FROM genres WHERE name = 'genretest0'), '13894e56-08d1-4c1f-b3e4-466c069d15ed', 1);

-------------------------------------------------------- tracksFollowed ------------------------------------------------
INSERT INTO tracksfollowed(userId, trackId)
  VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '02894e56-08d1-4c1f-b3e4-466c069d15ed');

-------------------------------------------------------- tracksRemoved -------------------------------------------------
INSERT INTO tracksrating(userId, trackId, reason)
  VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '13894e56-08d1-4c1f-b3e4-466c069d15ed', 'a');

-------------------------------------------------------- issuesComments ------------------------------------------------
INSERT INTO issuescomments(commentId, content, userid, issueid)
  VALUES(100, 'content', '077f3ea6-2272-4457-a47e-9e9111108e44', 100);

-------------------------------------------------------- facebookAttendees ---------------------------------------------
INSERT INTO facebookAttendees(id, attendeeFacebookId, name)
  VALUES(100, 'abcdefghij', 'name100');

-------------------------------------------------------- facebookAttendees ---------------------------------------------
INSERT INTO facebookAttendeeEventRelations(attendeeFacebookId, eventFacebookId, attendeeStatus)
  VALUES('abcdefghij', 'facebookidattendeetest', 'D');


# --- !Downs
DROP TABLE IF EXISTS tracksFollowed, tracksRating;
DROP TABLE IF EXISTS usersPlaylists;
DROP TABLE IF EXISTS playlistsTracks;
DROP TABLE IF EXISTS eventsGenres;
DROP TABLE IF EXISTS eventsPlaces;
DROP TABLE IF EXISTS eventsOrganizers;
DROP TABLE IF EXISTS eventsAddresses;
DROP TABLE IF EXISTS usersOrganizers;
DROP TABLE IF EXISTS eventsArtists;
DROP TABLE IF EXISTS artistsGenres;
DROP TABLE IF EXISTS tracksGenres;
DROP TABLE IF EXISTS eventsFollowed, facebookAttendeeEventRelations;
DROP TABLE IF EXISTS artistsFollowed;
DROP TABLE IF EXISTS placesFollowed;
DROP TABLE IF EXISTS usersFollowed;
DROP TABLE IF EXISTS organizersFollowed;
DROP TABLE IF EXISTS usersTools;
DROP TABLE IF EXISTS ticketStatuses;
DROP TABLE IF EXISTS blockedTickets;
DROP TABLE IF EXISTS boughtTicketBills;
DROP TABLE IF EXISTS soldTicketBills;
DROP TABLE IF EXISTS pendingTickets;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS salableEvents;
DROP TABLE IF EXISTS tariffsBlocked;
DROP TABLE IF EXISTS tariffs;
DROP TABLE IF EXISTS bank;
DROP TABLE IF EXISTS account411;
DROP TABLE IF EXISTS account413;
DROP TABLE IF EXISTS account4191;
DROP TABLE IF EXISTS account403;
DROP TABLE IF EXISTS account457;
DROP TABLE IF EXISTS account60;
DROP TABLE IF EXISTS account626;
DROP TABLE IF EXISTS account627;
DROP TABLE IF EXISTS account623;
DROP TABLE IF EXISTS account4686;
DROP TABLE IF EXISTS account63;
DROP TABLE IF EXISTS account708;
DROP TABLE IF EXISTS issuesComments;
DROP TABLE IF EXISTS issues;
DROP TABLE IF EXISTS receivedMails;
DROP TABLE IF EXISTS usersTools;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS playlists;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS infos;
DROP TABLE IF EXISTS places;
DROP TABLE IF EXISTS amountDue;
DROP TABLE IF EXISTS genres;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS users_token;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS frenchCities;
DROP TABLE IF EXISTS users, logininfo, userlogininfo, passwordinfo, oauth1info,  oauth2info, openidinfo, openidattributes;
DROP TABLE IF EXISTS facebookAttendees;

