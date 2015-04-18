# --- !Ups
CREATE TABLE frenchCities (
  cityId                    SERIAL PRIMARY KEY,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           POINT NOT NULL
);
CREATE INDEX frenchCityGeographicPoints ON frenchCities USING GIST (geographicPoint);
CREATE INDEX frenchCityNames ON frenchCities (name);

CREATE TABLE addresses (
  addressId                 SERIAL PRIMARY KEY,
  geographicPoint           POINT,
  city                      VARCHAR(127),
  zip                       VARCHAR(15),
  street                    VARCHAR
);
CREATE INDEX geographicPointAdresses ON addresses USING GIST (geographicPoint);

CREATE OR REPLACE FUNCTION insertAddress(
  geographicPointValue      VARCHAR(63),
  cityValue                 VARCHAR(127),
  zipValue                  VARCHAR(15),
  streetValue               VARCHAR)
  RETURNS INT AS
  $$
  DECLARE addressIdToReturn int;;
  BEGIN
    INSERT INTO addresses (geographicPoint, city, zip, street)
      VALUES (POINT(geographicPointValue), cityValue, zipValue, streetValue)
    RETURNING addressId INTO addressIdToReturn;;
    RETURN addressIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE orders ( --account701
  orderId                   SERIAL PRIMARY KEY,
  totalPrice                INT NOT NULL
);

CREATE TABLE infos (
  infoId                    SERIAL PRIMARY KEY,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR
);
INSERT INTO infos (title, content) VALUES ('Bienvenue', 'Jetez un oeil, ça vaut le détour');
INSERT INTO infos (title, content) VALUES (':) :) :)', 'Déjà deux utilisateurs !!!');
INSERT INTO infos (title, content) VALUES ('Timeline', 'm - 7 avant la bêta :) :)');
INSERT INTO infos (title, content) VALUES ('TicketApp', 'Cest simple, cest beau, ça fuse');

CREATE TABLE artists (
  artistId                  SERIAL PRIMARY KEY,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  facebookId                VARCHAR(63),
  name                      VARCHAR(255) NOT NULL,
  imagePath                 VARCHAR,
  description               VARCHAR,
  facebookUrl               VARCHAR(255) NOT NULL,
  websites                  VARCHAR,
  UNIQUE(facebookId),
  UNIQUE(facebookUrl)
);

CREATE OR REPLACE FUNCTION insertArtist(facebookIdValue VARCHAR(63),
                                        nameValue VARCHAR(255),
                                        imagePathValue VARCHAR,
                                        descriptionValue VARCHAR,
                                        facebookUrlValue VARCHAR(255),
                                        websitesValue VARCHAR)
  RETURNS INT AS
  $$
  DECLARE artistIdToReturn int;;

  BEGIN
    INSERT INTO artists (facebookId, name, imagePath, description, facebookUrl, websites)
    VALUES (facebookIdValue, nameValue, imagePathValue, descriptionValue, facebookUrlValue, websitesValue)
    RETURNING artistId INTO artistIdToReturn;;
    RETURN artistIdToReturn;;
    EXCEPTION WHEN unique_violation
    THEN
      SELECT artistId INTO artistIdToReturn FROM artists WHERE facebookId = facebookIdValue;;
      RETURN artistIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

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
  placeId                 BIGINT,
  UNIQUE(facebookId),
  UNIQUE(name),
  UNIQUE(placeId)
);

CREATE OR REPLACE FUNCTION insertOrganizer(
  facebookIdValue    VARCHAR(63),
  nameValue          VARCHAR(255),
  descriptionValue   VARCHAR,
  phoneValue         VARCHAR(255),
  publicTransitValue VARCHAR,
  websitesValue      VARCHAR,
  imagePathValue     VARCHAR,
  placeIdValue       BIGINT)
  RETURNS INT AS
  $$
  DECLARE organizerIdToReturn int;;
  BEGIN
    INSERT INTO organizers (facebookId, name, description, phone, publicTransit, websites, imagePath, placeId)
    VALUES (facebookIdValue, nameValue, descriptionValue, phoneValue, publicTransitValue, websitesValue, imagePathValue, placeIdValue)
    RETURNING organizerId
      INTO organizerIdToReturn;;
    RETURN organizerIdToReturn;;
    EXCEPTION WHEN unique_violation
    THEN
      SELECT organizerId
      INTO organizerIdToReturn
      FROM organizers
      WHERE facebookId = facebookIdValue;;
      RETURN organizerIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;;

CREATE TABLE genres (
  genreId                 SERIAL PRIMARY KEY,
  name                    CITEXT NOT NULL,
  icon                    CHAR,
  UNIQUE(name)
);

CREATE OR REPLACE FUNCTION insertGenre(nameValue VARCHAR(255), iconValue VARCHAR) RETURNS INT AS
  $$
  DECLARE genreIdToReturn int;;

  BEGIN
    INSERT INTO genres (NAME, icon) VALUES (nameValue, iconValue::CHAR) RETURNING genreId INTO genreIdToReturn;;
      RETURN genreIdToReturn;;
      EXCEPTION WHEN unique_violation
    THEN
      SELECT genreId INTO genreIdToReturn FROM genres WHERE name = nameValue;;
      RETURN genreIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE tracks (
  trackId                 SERIAL PRIMARY KEY,
  title                   VARCHAR(255) NOT NULL,
  url                     VARCHAR NOT NULL,
  platform                VARCHAR(255) NOT NULL,
  thumbnailUrl            VARCHAR NOT NULL,
  artistFacebookUrl       VARCHAR(255) REFERENCES artists(facebookUrl) NOT NULL,
  redirectUrl             VARCHAR(255),
  UNIQUE(url)
);
CREATE INDEX artistFacebookUrl ON tracks(artistFacebookUrl);

CREATE OR REPLACE FUNCTION insertTrack(titleValue VARCHAR(255),
                                       urlValue VARCHAR,
                                       platformValue VARCHAR(255),
                                       thumbnailUrlValue VARCHAR,
                                       artistFacebookUrlValue VARCHAR(255),
                                       redirectUrlValue VARCHAR(255))
  RETURNS INT AS
  $$
  DECLARE trackIdToReturn int;;
  BEGIN
    INSERT INTO tracks (title, url, platform, thumbnailUrl, artistFacebookUrl, redirectUrl)
      VALUES (titleValue, urlValue, platformValue, thumbnailUrlValue, artistFacebookUrlValue, redirectUrlValue)
      RETURNING trackId INTO trackIdToReturn;;
      RETURN trackIdToReturn;;
    EXCEPTION WHEN unique_violation THEN
      SELECT trackId INTO trackIdToReturn FROM tracks WHERE url = urlValue;;
      RETURN trackIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE users (
  userId                    SERIAL PRIMARY KEY,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  email                     VARCHAR(255) NOT NULL,
  nickname                  VARCHAR(255) NOT NULL,
  password                  VARCHAR(255) NOT NULL,
  profile                   VARCHAR(255) NOT NULL,
  UNIQUE(email)
);
INSERT INTO users (email, nickname, password, profile)
VALUES ('admin@global.local', 'admin', '$2a$12$L/rFVHZonEAmydEfZyYR.exvJuDdMY6kX7BIdXcam.voTxeBc7YwK', 'Admin');
INSERT INTO users (email, nickname, password, profile)
VALUES ('user@global.local', 'user', '$2a$12$3.UvEUatM.2VbYEI2Y.YKeqn3QNc/k0h9S0Vde2vqvzScKt74ofaS', 'User');

CREATE TABLE users_login (
  id                        SERIAL PRIMARY KEY,
  userId                    VARCHAR(255) NOT NULL,
  providerId                VARCHAR(255) NOT NULL,
  firstName                 VARCHAR(255) NOT NULL,
  lastName                  VARCHAR(255) NOT NULL,
  fullName                  VARCHAR(255) NOT NULL,
  email                     VARCHAR(255),
  avatarUrl                 VARCHAR,
  authMethod                VARCHAR(255) NOT NULL,
  oAuth1Info                VARCHAR,
  oAuth2Info                VARCHAR,
  passwordInfo              VARCHAR(255),
  UNIQUE(userId)
);

CREATE TABLE users_token (
  id                        VARCHAR(36) NOT NULL,
  email                     VARCHAR(255) NOT NULL,
  creationTime              TIMESTAMP NOT NULL,
  expirationTime            TIMESTAMP NOT NULL,
  isSignUp                  BOOLEAN NOT NULL
);

CREATE TABLE receivedMails (
  id                        SERIAL PRIMARY KEY,
  subject                   VARCHAR NOT NULL,
  message                   VARCHAR NOT NULL,
  read                      BOOLEAN NOT NULL DEFAULT FALSE,
  userId                    VARCHAR(255) REFERENCES users_login(userId)
);

CREATE TABLE events (
  eventId                   SERIAL PRIMARY KEY,
  facebookId                VARCHAR(63),
  isPublic                  BOOLEAN NOT NULL,
  isActive                  BOOLEAN NOT NULL,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           POINT,
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

CREATE OR REPLACE FUNCTION insertEvent(
  facebookIdValue                VARCHAR(63),
  isPublicValue                  BOOLEAN,
  isActiveValue                  BOOLEAN,
  nameValue                      VARCHAR(255),
  geographicPointValue           VARCHAR(63),
  descriptionValue               VARCHAR,
  startTimeValue                 TIMESTAMP with time zone,
  endTimeValue                   TIMESTAMP with time zone,
  imagePathValue                 VARCHAR,
  ageRestrictionValue            INT,
  tariffRangeValue               VARCHAR(15),
  ticketSellersValue             VARCHAR)
  RETURNS INT AS
  $$
  DECLARE eventIdToReturn int;;
  BEGIN
    INSERT INTO events (facebookid, ispublic, isactive, name, geographicpoint, description, starttime,
                        endtime, imagePath, agerestriction, tariffRange, ticketSellers)
    VALUES (facebookidValue, ispublicValue, isactiveValue, nameValue, POINT(geographicpointValue),
            descriptionValue, starttimeValue, endtimeValue, imagePathValue, agerestrictionValue::SMALLINT,
            tariffRangeValue, ticketSellersValue)
    RETURNING eventId INTO eventIdToReturn;;
    RETURN eventIdToReturn;;
    EXCEPTION WHEN unique_violation THEN
    SELECT eventId INTO eventIdToReturn FROM events WHERE facebookId = facebookIdValue;;
    RETURN eventIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE places (
  placeId                   SERIAL PRIMARY KEY,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           POINT,
  addressId                 BIGINT references addresses(addressId),
  facebookId                VARCHAR(63),
  description               VARCHAR,
  webSites                  VARCHAR,
  facebookMiniature         VARCHAR,
  capacity                  INT,
  openingHours              VARCHAR(255),
  imagePath                 VARCHAR,
  organizerId               BIGINT,
  UNIQUE(facebookId),
  UNIQUE(organizerId)
);
CREATE INDEX placeGeographicPoint ON places USING GIST (geographicPoint);
INSERT into places(name, geographicPoint, facebookId)
values ('Le transbordeur', POINT('(45.783808,4.860598)'), '117030545096697');

CREATE OR REPLACE FUNCTION insertPlace(
  nameValue                      VARCHAR(255),
  geographicPointValue           VARCHAR(63),
  addressIdValue                 INT,
  facebookIdValue                VARCHAR(63),
  descriptionValue               VARCHAR,
  webSitesValue                  VARCHAR,
  capacityValue                  INT,
  openingHoursValue              VARCHAR(255),
  imagePathValue                 VARCHAR,
  organizerIdValue               BIGINT)
  RETURNS INT AS
  $$
DECLARE placeIdToReturn int;;
  BEGIN
    INSERT INTO places (name, geographicPoint, addressId, facebookId, description, webSites,
                        capacity, openingHours, imagePath, organizerId)
    VALUES (nameValue, POINT(geographicPointValue), addressIdValue::BIGINT, facebookIdValue, descriptionValue,
            webSitesValue, capacityValue, openingHoursValue, imagePathValue, organizerIdValue)
    RETURNING placeId INTO placeIdToReturn;;
    RETURN placeIdToReturn;;
  EXCEPTION WHEN unique_violation THEN
    SELECT placeId INTO placeIdToReturn FROM places WHERE facebookId = facebookIdValue;;
    RETURN placeIdToReturn;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE images (
  imageId                   SERIAL PRIMARY KEY,
  path                      VARCHAR NOT NULL,
  category                  VARCHAR(31),
  userId                    BIGINT references users(userId),
  organizerId               BIGINT references organizers(organizerId),
  infoId                    BIGINT references infos(infoId),
  trackId                   BIGINT references tracks(trackId),
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
  userId                    VARCHAR(255) REFERENCES users_login (userId),
  fixed                     BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE issuesComments (
  commentId                 SERIAL PRIMARY KEY,
  content                   VARCHAR,
  userId                    VARCHAR(255) REFERENCES users_login (userId),
  issueId                   BIGINT REFERENCES issues(issueId)
);

CREATE TABLE usersTools (
  tools                     VARCHAR(255) NOT NULL,
  userId                    BIGINT REFERENCES users(userId),
  PRIMARY KEY (userId)
);
INSERT INTO usersTools (tools, userId) VALUES ('tool1, tool2, tool3', 1);

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
  debit                   Boolean NOT NULL,
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
  debit                   Boolean NOT NULL, --#sinon debit
  userId                  BIGINT REFERENCES users(userId),
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
  userId                   VARCHAR REFERENCES users_login(userId),
  eventId                  BIGINT REFERENCES events(eventId),
  PRIMARY KEY (userId, eventId)
);

CREATE TABLE artistsFollowed (
  userId                   VARCHAR REFERENCES users_login(userId),
  artistId                 INT REFERENCES artists(artistId),
  PRIMARY KEY (userId, artistId)
);

CREATE TABLE placesFollowed (
  userId                   VARCHAR REFERENCES users_login(userId),
  placeId                  INT REFERENCES places(placeId),
  PRIMARY KEY (userId, placeId)
);

CREATE TABLE usersFollowed (
  userIdFollower          VARCHAR REFERENCES users_login(userId),
  userIdFollowed          VARCHAR REFERENCES users_login(userId),
  PRIMARY KEY (userIdFollower, userIdFollowed)
);

CREATE TABLE eventsPlaces (
  eventId                 INT REFERENCES events (eventId),
  placeId                 INT REFERENCES places (placeId),
  PRIMARY KEY (eventId, placeId)
);
CREATE OR REPLACE FUNCTION insertEventPlaceRelation(
  eventIdValue            BIGINT,
  placeIdValue            BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO eventsPlaces (eventId, placeId)
    VALUES (eventIdValue, placeIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;


CREATE TABLE eventsGenres (
  eventId                 INT REFERENCES events (eventId),
  genreId                 INT REFERENCES genres (genreId),
  PRIMARY KEY (eventId, genreId)
);
CREATE OR REPLACE FUNCTION insertEventGenreRelation(
  eventIdValue            BIGINT,
  genreIdValue            BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO eventsGenres (eventId, genreId)
    VALUES (eventIdValue, genreIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE eventsOrganizers (
  eventId                 INT REFERENCES events (eventId),
  organizerId             INT REFERENCES organizers(organizerId),
  PRIMARY KEY (eventId, organizerId)
);
CREATE OR REPLACE FUNCTION insertEventOrganizerRelation(
  eventIdValue            BIGINT,
  organizerIdValue        BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO eventsOrganizers (eventId, organizerId)
    VALUES (eventIdValue, organizerIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE eventsAddresses (
  eventId                 INT REFERENCES events (eventId),
  addressId               INT REFERENCES addresses(addressId),
  PRIMARY KEY (eventId, addressId)
);
---INSERT INTO eventsAddresses VALUES(1, 2);

CREATE TABLE usersOrganizers (
  userId                  INT REFERENCES users (userId),
  organizerId             INT REFERENCES organizers(organizerId),
  PRIMARY KEY (userId, organizerId)
);

CREATE TABLE eventsArtists (
  eventId                 INT REFERENCES events (eventId),
  artistId                INT REFERENCES artists (artistId),
  PRIMARY KEY (eventId, artistId)
);
CREATE OR REPLACE FUNCTION insertEventArtistRelation(
  eventIdValue            BIGINT,
  artistIdValue           BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO eventsArtists (eventId, artistId)
    VALUES (eventIdValue, artistIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE usersArtists (
  userId                  INT REFERENCES users (userId),
  artistId                INT REFERENCES artists (artistId),
  PRIMARY KEY (userId, artistId)
);

CREATE TABLE artistsGenres (
  artistId                INT REFERENCES artists (artistId),
  genreId                 INT REFERENCES genres (genreId),
  counter                 INT NOT NULL,
  PRIMARY KEY (artistId, genreId)
);

CREATE OR REPLACE FUNCTION insertOrUpdateArtistGenreRelation(artistIdValue INT, genreIdValue INT) RETURNS VOID AS
  $$
    BEGIN
      LOOP
        UPDATE artistsGenres SET counter = counter + 1 WHERE artistId = artistIdValue AND genreId = genreIdValue;;
        IF found THEN
          RETURN;;
        END IF;;
        BEGIN
          INSERT INTO artistsGenres(artistId, genreId, counter) VALUES (artistIdValue, genreIdValue, 1);;
          RETURN;;
        EXCEPTION WHEN unique_violation THEN
        END;;
      END LOOP;;
    END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE playlists (
  playlistId              SERIAL PRIMARY KEY,
  userId                  VARCHAR(255) REFERENCES users_login (userId),
  name                    VARCHAR(255)
);

CREATE TABLE playlistsTracks (
  playlistId              BIGINT REFERENCES playlists (playlistId),
  trackId                 BIGINT REFERENCES tracks (trackId),
  trackRank               FLOAT NOT NULL,
  PRIMARY KEY (playlistId, trackId)
);

# --- !Downs
DROP TABLE IF EXISTS usersPlaylists;
DROP TABLE IF EXISTS playlistsTracks;
DROP TABLE IF EXISTS eventsGenres;
DROP TABLE IF EXISTS eventsPlaces;
DROP TABLE IF EXISTS eventsOrganizers;
DROP TABLE IF EXISTS eventsAddresses;
DROP TABLE IF EXISTS usersOrganizers;
DROP TABLE IF EXISTS eventsArtists;
DROP TABLE IF EXISTS usersArtists;
DROP TABLE IF EXISTS artistsGenres;
DROP TABLE IF EXISTS eventsFollowed;
DROP TABLE IF EXISTS artistsFollowed;
DROP TABLE IF EXISTS placesFollowed;
DROP TABLE IF EXISTS usersFollowed;
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
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS users_login, users_token;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS frenchCities;


