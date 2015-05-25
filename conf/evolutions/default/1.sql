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
CREATE UNIQUE INDEX addressesIndex ON addresses (city, zip, street);

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
    EXCEPTION WHEN unique_violation
      THEN
        SELECT addressId INTO addressIdToReturn FROM addresses
          WHERE city = cityValue AND zip = zipValue AND street = streetValue;;
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
  displayIfConnected        BOOLEAN NOT NULL DEFAULT TRUE,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  animationContent          VARCHAR,
  animationStyle            VARCHAR
);

INSERT INTO infos (title, content) VALUES ('Timeline', 's - 13 avant la bêta :) :)');
INSERT INTO infos (title, content) VALUES ('Bienvenue', 'Jetez un oeil, ça vaut le détour');
INSERT INTO infos (title, content) VALUES (':) :) :)', 'Déjà deux utilisateurs !!!');
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
  likes                     INTEGER,
  country                   VARCHAR,
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
  geographicPoint         POINT,
  placeId                 BIGINT,
  UNIQUE(facebookId),
  UNIQUE(name),
  UNIQUE(placeId)
);

CREATE OR REPLACE FUNCTION insertOrganizer(
  facebookIdValue       VARCHAR(63),
  nameValue             VARCHAR(255),
  descriptionValue      VARCHAR,
  addressIdValue        BIGINT,
  phoneValue            VARCHAR(255),
  publicTransitValue    VARCHAR,
  websitesValue         VARCHAR,
  imagePathValue        VARCHAR,
  geographicPointValue  VARCHAR(63),
  placeIdValue          BIGINT)
  RETURNS INT AS
  $$
  DECLARE organizerIdToReturn int;;
  BEGIN
    INSERT INTO organizers (facebookId, name, description, addressId, phone, publicTransit, websites, imagePath,
                            geographicPoint, placeId)
    VALUES (facebookIdValue, nameValue, descriptionValue, addressIdValue, phoneValue, publicTransitValue,
            websitesValue, imagePathValue, POINT(geographicPointValue), placeIdValue)
    RETURNING organizerId
      INTO organizerIdToReturn;;
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
  trackId                 VARCHAR(255) NOT NULL,
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
CREATE UNIQUE INDEX trackId ON tracks(trackId);
CREATE UNIQUE INDEX artistNameAndTitle ON tracks(title, artistName);
CREATE INDEX artistFacebookUrl ON tracks(artistFacebookUrl);

CREATE OR REPLACE FUNCTION insertTrack(trackIdValue VARCHAR(255),
                                       titleValue VARCHAR(255),
                                       urlValue VARCHAR,
                                       platformValue CHAR,
                                       thumbnailUrlValue VARCHAR,
                                       artistFacebookUrlValue VARCHAR(255),
                                       artistNameValue VARCHAR(255),
                                       redirectUrlValue VARCHAR(255))
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO tracks (trackId, title, url, platform, thumbnailUrl, artistFacebookUrl, artistName, redirectUrl)
    VALUES (trackIdValue, titleValue, urlValue, platformValue, thumbnailUrlValue, artistFacebookUrlValue,
            artistNameValue, redirectUrlValue);;
  END;;
  $$
LANGUAGE plpgsql;

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
INSERT INTO users_login(userId, providerId, firstName, lastName, fullName, authMethod)
VALUES ('userTestId', 'providerId', 'firstName', 'lastName', 'fullName', 'oauth2');

CREATE OR REPLACE FUNCTION insertUser(
  userIdValue         VARCHAR(255),
  providerIdValue     VARCHAR(255),
  firstNameValue      VARCHAR(255),
  lastNameValue       VARCHAR(255),
  fullNameValue       VARCHAR(255),
  emailValue          VARCHAR(255),
  avatarUrlValue      VARCHAR,
  authMethodValue     VARCHAR(255),
  oAuth1InfoValue     VARCHAR,
  oAuth2InfoValue     VARCHAR,
  passwordInfoValue   VARCHAR(255))
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO users_login (userId, providerId, firstName, lastName, fullName, email, avatarUrl, authMethod,
                             oAuth1Info, oAuth2Info, passwordInfo)
      VALUES (userIdValue, providerIdValue, firstNameValue, lastNameValue, fullNameValue, emailValue, avatarUrlValue,
              authMethodValue, oAuth1InfoValue, oAuth2InfoValue, passwordInfoValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;


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
-- SELECT insertEvent('facebookId', true, true, 'name', '(0,0)', 'description', current_timestamp,
--                    current_timestamp + interval '2000000 hour', 'imagePath', 16, '5-10', 'ticketSeller');


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
  addressIdValue                 BIGINT,
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
    VALUES (nameValue, POINT(geographicPointValue), addressIdValue, facebookIdValue, descriptionValue,
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
  organizerId               BIGINT REFERENCES organizers(organizerId),
  infoId                    BIGINT REFERENCES infos(infoId),
  trackId                   VARCHAR(255) REFERENCES tracks(trackId),
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
  tableId                   SERIAL PRIMARY KEY,
  tools                     VARCHAR(255) NOT NULL,
  userId                    VARCHAR(255) REFERENCES users_login (userId)
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
  userId                  VARCHAR(255) REFERENCES users_login(userId),
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
  userId                   VARCHAR REFERENCES users_login(userId),
  eventId                  BIGINT REFERENCES events(eventId)
);
CREATE UNIQUE INDEX eventsFollowedIndex ON eventsFollowed (userId, eventId);

CREATE TABLE artistsFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   VARCHAR REFERENCES users_login(userId),
  artistId                 INT REFERENCES artists(artistId)
);
CREATE UNIQUE INDEX artistsFollowedIndex ON artistsFollowed (userId, artistId);
CREATE OR REPLACE FUNCTION insertUserArtistRelation(
  userIdValue             VARCHAR(255),
  artistIdValue           BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO artistsFollowed (userId, artistId)
      VALUES (userIdValue, artistIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;

CREATE TABLE placesFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   VARCHAR REFERENCES users_login(userId) NOT NULL,
  placeId                  INT REFERENCES places(placeId)  NOT NULL
);
CREATE UNIQUE INDEX placesFollowedIndex ON placesFollowed (userId, placeId);

CREATE TABLE usersFollowed (
  tableId                 SERIAL PRIMARY KEY,
  userIdFollower          VARCHAR REFERENCES users_login(userId)  NOT NULL,
  userIdFollowed          VARCHAR REFERENCES users_login(userId)  NOT NULL
);
CREATE UNIQUE INDEX usersFollowedIndex ON usersFollowed (userIdFollower, userIdFollowed);

CREATE TABLE organizersFollowed (
  tableId                 SERIAL PRIMARY KEY,
  userId                  VARCHAR REFERENCES users_login(userId)  NOT NULL,
  organizerId             INT REFERENCES organizers(organizerId)  NOT NULL
);
CREATE UNIQUE INDEX organizersFollowedIndex ON organizersFollowed (userId, organizerId);

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

CREATE TABLE usersOrganizers (
  tableId                 SERIAL PRIMARY KEY,
  userId                  VARCHAR(255) REFERENCES users_login (userId),
  organizerId             INT REFERENCES organizers(organizerId)
);
CREATE UNIQUE INDEX usersOrganizersIndex ON usersOrganizers (userId, organizerId);
CREATE OR REPLACE FUNCTION insertUserOrganizerRelation(
  userIdValue             VARCHAR(255),
  organizerIdValue        BIGINT)
  RETURNS VOID AS
  $$
  BEGIN
    INSERT INTO usersOrganizers (userId, organizerId)
    VALUES (userIdValue, organizerIdValue);;
    EXCEPTION WHEN unique_violation THEN RETURN;;
  END;;
  $$
LANGUAGE plpgsql;


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


CREATE TABLE artistsGenres (
  artistId                INT REFERENCES artists (artistId),
  genreId                 INT REFERENCES genres (genreId),
  counter                 INT NOT NULL,
  PRIMARY KEY (artistId, genreId)
);
CREATE OR REPLACE FUNCTION insertOrUpdateArtistGenreRelation(artistIdValue BIGINT, genreIdValue BIGINT) RETURNS VOID AS
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
  userId                  VARCHAR(255) REFERENCES users_login (userId) NOT NULL,
  name                    VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX playlistsIndex ON playlists (playlistId, userId);


CREATE TABLE playlistsTracks (
  playlistId              BIGINT REFERENCES playlists (playlistId),
  trackId                 VARCHAR(255) REFERENCES tracks (trackId),
  trackRank               FLOAT NOT NULL,
  PRIMARY KEY (playlistId)
);
CREATE UNIQUE INDEX playlistsTracksIndex ON playlistsTracks (playlistId, trackId);


CREATE TABLE tracksRating (
  tableId                 SERIAL PRIMARY KEY,
  userId                  VARCHAR(255) REFERENCES users_login (userId) NOT NULL,
  trackId                 VARCHAR(255) REFERENCES tracks (trackId) NOT NULL,
  ratingUp                INT,
  ratingDown              INT,
  reason                  CHAR
);
CREATE UNIQUE INDEX tracksRatingIndex ON tracksRating (userId, trackId);
CREATE OR REPLACE FUNCTION upsertTrackRatingUp(
  userIdValue     VARCHAR(255),
  trackIdValue    VARCHAR(255),
  ratingUpValue   INT)
  RETURNS VOID AS
  $$
    BEGIN
      LOOP
        UPDATE tracksRating SET ratingUp = ratingUp + ratingUpValue
          WHERE trackId = trackIdValue AND userId = userIdValue;;
        IF found THEN
          RETURN;;
        END IF;;
        BEGIN
          INSERT INTO tracksRating(trackId, userId, ratingUp) VALUES (trackIdValue, userIdValue, ratingUpValue);;
          RETURN;;
        EXCEPTION WHEN unique_violation THEN
        END;;
      END LOOP;;
    END;;
  $$
LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION upsertTrackRatingDown(
  userIdValue       VARCHAR(255),
  trackIdValue      VARCHAR(255),
  ratingDownValue   INT)
  RETURNS VOID AS
  $$
    BEGIN
      LOOP
        UPDATE tracksRating SET ratingDown = ratingDown + ratingDownValue 
          WHERE trackId = trackIdValue AND userId = userIdValue;;
        IF found THEN
          RETURN;;
        END IF;;
        BEGIN
          INSERT INTO tracksRating(trackId, userId, ratingDown) VALUES (trackIdValue, userIdValue, ratingDownValue);;
          RETURN;;
        EXCEPTION WHEN unique_violation THEN
        END;;
      END LOOP;;
    END;;
  $$
LANGUAGE plpgsql;


CREATE TABLE usersFavoriteTracks(
  tableId                 SERIAL PRIMARY KEY,
  userId                  VARCHAR(255) REFERENCES users_login (userId) NOT NULL,
  trackId                 VARCHAR(255) REFERENCES tracks (trackId) NOT NULL
);
CREATE UNIQUE INDEX usersFavoriteTracksIndex ON usersFavoriteTracks (userId, trackId);

# --- !Downs
DROP TABLE IF EXISTS usersFavoriteTracks;
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
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS users_login, users_token;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS frenchCities;


