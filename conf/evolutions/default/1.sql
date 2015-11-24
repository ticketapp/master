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
  geographicPoint           GEOMETRY,
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
  addressId               BIGINT references addresses(addressId),
  phone                   VARCHAR(255),
  publicTransit           VARCHAR,
  websites                VARCHAR,
  verified                BOOLEAN DEFAULT FALSE NOT NULL,
  imagePath               VARCHAR,
  geographicPoint         GEOMETRY,
  placeId                 BIGINT,
  UNIQUE(facebookId)
);


CREATE TABLE genres (
  genreId                 SERIAL PRIMARY KEY,
  name                    CITEXT NOT NULL,
  icon                    CHAR NOT NULL,
  UNIQUE(name)
);


CREATE TABLE tracks (
  trackId                 UUID PRIMARY KEY NOT NULL,
  title                   VARCHAR(255) NOT NULL,
  url                     VARCHAR NOT NULL,
  platform                CHAR NOT NULL,
  thumbnailUrl            VARCHAR NOT NULL,
  artistFacebookUrl       VARCHAR(255) REFERENCES artists(facebookUrl) NOT NULL,
  artistName              VARCHAR(255) NOT NULL,
  redirectUrl             VARCHAR(255),
  confidence              DOUBLE PRECISION NOT NULL DEFAULT 0,
  ratingUp                INT NOT NULL DEFAULT 0,
  ratingDown              INT NOT NULL DEFAULT 0,
  UNIQUE(url)
);
CREATE UNIQUE INDEX artistNameAndTitle ON tracks(title, artistName);
CREATE INDEX artistFacebookUrl ON tracks(artistFacebookUrl);


create table users (
  userID                    UUID PRIMARY KEY,
  firstName                 VARCHAR,
  lastName                  VARCHAR,
  fullName                  VARCHAR,
  email                     VARCHAR UNIQUE,
  avatarURL                 VARCHAR
);


CREATE TABLE logininfo (
  id                       SERIAL PRIMARY KEY,
  providerID               VARCHAR NOT NULL,
  providerKey              VARCHAR NOT NULL
);


CREATE TABLE userlogininfo (
  userID                   UUID PRIMARY KEY,
  loginInfoId              BIGINT NOT NULL
);


CREATE TABLE passwordinfo (
  hasher VARCHAR NOT NULL,
  password VARCHAR NOT NULL,
  salt VARCHAR,
  loginInfoId BIGINT NOT NULL
);


CREATE TABLE oauth1info (id SERIAL PRIMARY KEY,token VARCHAR NOT NULL,secret VARCHAR NOT NULL,loginInfoId BIGINT NOT NULL);
CREATE TABLE oauth2info (id SERIAL PRIMARY KEY,accesstoken VARCHAR NOT NULL,tokentype VARCHAR,expiresin INTEGER,refreshtoken VARCHAR,logininfoid BIGINT NOT NULL);
CREATE TABLE openidinfo (id VARCHAR NOT NULL PRIMARY KEY,logininfoid BIGINT NOT NULL);
CREATE TABLE openidattributes (id VARCHAR NOT NULL,key VARCHAR NOT NULL,value VARCHAR NOT NULL);


CREATE TABLE receivedMails (
  id                        SERIAL PRIMARY KEY,
  subject                   VARCHAR NOT NULL,
  message                   VARCHAR NOT NULL,
  read                      BOOLEAN NOT NULL DEFAULT FALSE,
  userId                    UUID REFERENCES users(userId)
);

CREATE TABLE events (
  eventId                   SERIAL PRIMARY KEY,
  facebookId                VARCHAR(63),
  isPublic                  BOOLEAN NOT NULL,
  isActive                  BOOLEAN NOT NULL,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY,
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
  geographicPoint           GEOMETRY,
  addressId                 BIGINT references addresses(addressId),
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
INSERT into places(name, geographicPoint, facebookId)
values ('Le transbordeur', ST_GeomFromText('POINT(45.783808 4.860598)', 4326), '117030545096697');


CREATE TABLE images (
  imageId                   SERIAL PRIMARY KEY,
  path                      VARCHAR NOT NULL,
  category                  VARCHAR(31),
  organizerId               BIGINT REFERENCES organizers(organizerId),
  infoId                    BIGINT REFERENCES infos(infoId),
  trackId                   UUID REFERENCES tracks(trackId),
  UNIQUE(path)
);

CREATE TABLE tariffs (
  tariffId                 SERIAL PRIMARY KEY,
  denomination             VARCHAR(255) DEFAULT 'Basique' NOT NULL,
  nbTicketToSell           INT NOT NULL,
  nbTicketSold             INT DEFAULT 0 NOT NULL,
  price                    NUMERIC NOT NULL,
  startTime                TIMESTAMP NOT NULL,
  endTime                  TIMESTAMP NOT NULL,
  eventId                  BIGINT REFERENCES events(eventId)
);

CREATE TABLE tickets (
  ticketId                  SERIAL PRIMARY KEY,
  isValid                   BOOLEAN DEFAULT TRUE,
  qrCode                    VARCHAR(255) NOT NULL,
  firstName                 VARCHAR(255),
  lastName                  VARCHAR(255),
  tariffId                  INT REFERENCES tariffs(tariffId),
  orderId                   INT REFERENCES orders(orderId)
);
--INSERT INTO tickets (tariffId, orderId) VALUES (1, 1);

---CREATE TABLE tariffsBlocked (
---  tariffsBlockedId         SERIAL PRIMARY KEY,
---  endTime                  TIMESTAMP DEFAULT current_timestamp + time '00:15' NOT NULL,
---  tariffId                 BIGINT REFERENCES tariffs(tariffId)
---);

CREATE TABLE issues (
  issueId                   SERIAL PRIMARY KEY,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId),
  fixed                     BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE issuesComments (
  commentId                 SERIAL PRIMARY KEY,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId),
  issueId                   BIGINT REFERENCES issues(issueId)
);

CREATE TABLE usersTools (
  tableId                   SERIAL PRIMARY KEY,
  tools                     VARCHAR(255) NOT NULL,
  userId                    UUID REFERENCES users(userId)
);

---############################## ACCOUNTING ###################################

---Vente de produits finis = account701 = table order

CREATE TABLE clients (
  clientId                SERIAL PRIMARY KEY,
  name                    VARCHAR(255),
  contactName             VARCHAR(255),
  socialDenomination      VARCHAR(255),
  addressID               BIGINT references addresses(addressID),
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
  orderId                 BIGINT references orders(orderId),
  account708Id            BIGINT references account708(id)
);

--diverses charges à payer
CREATE TABLE account4686 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255),
  amount                  NUMERIC NOT NULL,
  debit                   BOOLEAN NOT NULL,
  account63Id             BIGINT references account63(id)
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
  userId                   UUID REFERENCES users(userId),
  eventId                  BIGINT REFERENCES events(eventId)
);
CREATE UNIQUE INDEX eventsFollowedIndex ON eventsFollowed (userId, eventId);

CREATE TABLE artistsFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId),
  artistId                 INT REFERENCES artists(artistId)
);
CREATE UNIQUE INDEX artistsFollowedIndex ON artistsFollowed (userId, artistId);


CREATE TABLE placesFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId),
  placeId                  INT REFERENCES places(placeId)  NOT NULL
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
  userId                  UUID REFERENCES users(userId),
  organizerId             INT REFERENCES organizers(organizerId)  NOT NULL
);
CREATE UNIQUE INDEX organizersFollowedIndex ON organizersFollowed (userId, organizerId);

CREATE TABLE tracksFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId),
  trackId                  UUID REFERENCES tracks(trackId)
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
  userId                  UUID REFERENCES users(userId),
  organizerId             INT REFERENCES organizers(organizerId)
);
CREATE UNIQUE INDEX usersOrganizersIndex ON usersOrganizers (userId, organizerId);


CREATE TABLE eventsArtists (
  eventId                 INT REFERENCES events (eventId) ON DELETE CASCADE,
  artistId                INT REFERENCES artists (artistId) ON DELETE CASCADE,
  PRIMARY KEY (eventId, artistId)
);


CREATE TABLE tracksGenres (
  trackId                 UUID REFERENCES tracks (trackId) NOT NULL,
  genreId                 INT REFERENCES genres (genreId) NOT NULL,
  weight                  BIGINT NOT NULL,
  PRIMARY KEY (genreId)
);
CREATE UNIQUE INDEX tracksGenresIndex ON tracksGenres (trackId, genreId);


CREATE TABLE artistsGenres (
  artistId                INT REFERENCES artists (artistId),
  genreId                 INT REFERENCES genres (genreId),
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
  userId                  UUID REFERENCES users(userId) NOT NULL,
  trackId                 UUID REFERENCES tracks (trackId) NOT NULL,
  ratingUp                INT,
  ratingDown              INT,
  reason                  CHAR
);
CREATE UNIQUE INDEX tracksRatingIndex ON tracksRating (userId, trackId);


# --- !Downs
DROP TABLE IF EXISTS tracksFollowed;
DROP TABLE IF EXISTS tracksRating;
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
DROP TABLE IF EXISTS eventsFollowed;
DROP TABLE IF EXISTS artistsFollowed;
DROP TABLE IF EXISTS placesFollowed;
DROP TABLE IF EXISTS usersFollowed;
DROP TABLE IF EXISTS organizersFollowed;
DROP TABLE IF EXISTS usersTools;
DROP TABLE IF EXISTS tickets;
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

