# --- !Ups
CREATE TABLE addresses (
  addressID                 SERIAL PRIMARY KEY,
  city                      VARCHAR(127) NOT NULL,
  CP                        VARCHAR(15) NOT NULL,
  address                   VARCHAR(255) NOT NULL
);
INSERT INTO addresses (city, CP, address) VALUES ('Lyon', 69001, 'adresse1');
INSERT INTO addresses (city, CP, address) VALUES ('Lyon', 69001, 'adresse2');
INSERT INTO addresses (city, CP, address) VALUES ('Lyon', 69001, 'adresse3');



CREATE TABLE orders ( --account701
  orderId                   SERIAL PRIMARY KEY,
  totalPrice                INT NOT NULL
);
INSERT INTO orders (totalPrice) VALUES (80);
INSERT INTO orders (totalPrice) VALUES (80);


CREATE TABLE comments (
  commentId                 SERIAL PRIMARY KEY,
  text                      VARCHAR(255) NOT NULL
);
INSERT INTO comments (text) VALUES ('un beau petit commentaire');

CREATE TABLE infos (
  infoId                    SERIAL PRIMARY KEY,
  title                     TEXT NOT NULL,
  content                   TEXT NOT NULL
);
INSERT INTO infos (title, content) VALUES ('Bienvenue', 'Ticketapp, la billetterie qui fuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuse');
INSERT INTO infos (title, content) VALUES ('infos2', 'une info qui va déchirer');
INSERT INTO infos (title, content) VALUES ('infos3', 'fuserie');

CREATE TABLE artists (
  artistId                  SERIAL PRIMARY KEY,
  creationDateTime          TIMESTAMP DEFAULT  current_timestamp NOT NULL,
  facebookId                BIGINT,
  name                      VARCHAR(255) NOT NULL
);
INSERT INTO artists (facebookId, name) VALUES (321, 'artists1');
INSERT INTO artists (facebookId, name) VALUES (654, 'artists2');
INSERT INTO artists (facebookId, name) VALUES (666, 'johny666');
INSERT INTO artists (facebookId, name) VALUES (77777, 'artists4');

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
  avatarUrl                 VARCHAR(255),
  authMethod                VARCHAR(255) NOT NULL,
  oAuth1Info                VARCHAR(255),
  oAuth2Info                VARCHAR(255),
  passwordInfo              VARCHAR(255)
);

CREATE TABLE users_token (
  id                        VARCHAR(36) NOT NULL,
  email                     VARCHAR(255) NOT NULL,
  creationTime              TIMESTAMP NOT NULL,
  expirationTime            TIMESTAMP NOT NULL,
  isSignUp                  BOOLEAN NOT NULL
);

CREATE TABLE events (
  eventId                   SERIAL PRIMARY KEY,
  isPublic                  boolean NOT NULL,
  isActive                  boolean NOT NULL,
  creationDateTime          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  startSellingTime          TIMESTAMP NOT NULL,
  endSellingTime            TIMESTAMP NOT NULL,
  description               TEXT NOT NULL,
  startTime                 TIMESTAMP NOT NULL,
  endTime                   TIMESTAMP NOT NULL,
  ageRestriction            SMALLINT NOT NULL
);
INSERT INTO events (isPublic, isActive, creationDateTime, name, startSellingTime,
                    endSellingTime, description, startTime,
                    endTime, ageRestriction)
VALUES (true, true, '1991-02-22 00:00:00', 'event1', '1991-02-22 00:00:00', '1991-02-22 00:00:00',
        'descriptionblablablalblagl zae az e', '1991-02-22 00:00:00',
        '1991-02-22 00:00:00', 16);
INSERT INTO events (isPublic, isActive, creationDateTime, name, startSellingTime,
                    endSellingTime, description, startTime,
                    endTime, ageRestriction)
VALUES (true, true, '1991-02-22 00:00:00', 'deuxieme', '1991-02-22 00:00:00', '1991-02-22 00:00:00',
        'descriptionbdlablablalblagl zae az e', '1991-02-22 00:00:00',
        '1991-02-22 00:00:00', 16);
INSERT INTO events (isPublic, isActive, creationDateTime, name, startSellingTime,
                    endSellingTime, description, startTime,
                    endTime, ageRestriction)
VALUES (true, true, '1991-02-22 00:00:00', 'troisieme3', '1991-02-22 00:00:00', '1991-02-22 00:00:00',
        'descriptionqsdqsdqsdqsdqsblablablalblagl zae az e', '1991-02-22 00:00:00',
        '1991-02-22 00:00:00', 16);
INSERT INTO events (isPublic, isActive, creationDateTime, name, startSellingTime,
                    endSellingTime, description, startTime,
                    endTime, ageRestriction)
VALUES (true, true, '1991-02-22 00:00:00', 'DJDKLS', '1991-02-22 00:00:00', '1991-02-22 00:00:00',
        'yoo oy yoyoyoyoyooyoyoy zae az e', '1991-02-22 00:00:00',
        '1991-02-22 00:00:00', 16);



CREATE TABLE places (
  placeId                   SERIAL PRIMARY KEY,
  name                      VARCHAR(255) NOT NULL,
  eventId                   BIGINT references events(eventId),
  addressID                 BIGINT references addresses(addressID)
);
INSERT INTO places (name, addressID) VALUES ('place1 ', 1);
INSERT INTO places (name, addressID) VALUES ('place2', 2);
INSERT INTO places (name, addressID) VALUES ('place3', 3);

CREATE TABLE images (
  imageId                   SERIAL PRIMARY KEY,
  path                      VARCHAR(255) NOT NULL,
  alt                       VARCHAR(255) NOT NULL,
  eventId                   BIGINT references events(eventId),
  userId                    BIGINT references users(userId),
  UNIQUE(path)
);
INSERT INTO images (path, alt, eventId, userId) VALUES ('1.jpg', 'alt', 1, 1);
INSERT INTO images (path, alt, eventId, userId) VALUES ('2.jpg', 'une jolie description', 2, 1);
INSERT INTO images (path, alt, eventId, userId) VALUES ('3.jpg', 'une belle description', 3, 2);
INSERT INTO images (path, alt, eventId) VALUES ('4.jpg', 'altqsdqsd', 4);
INSERT INTO images (path, alt, eventId) VALUES ('6.jpg', 'alt3', 1);

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
INSERT INTO tariffs (nbTicketToSell, price, startTime, endTime) VALUES (1, 1, current_timestamp, current_timestamp);

CREATE TABLE tickets (
  ticketId                  SERIAL PRIMARY KEY,
  isValid                   BOOLEAN DEFAULT TRUE,
  qrCode                    VARCHAR(255) NOT NULL,
  firstName                 VARCHAR(255),
  lastName                  VARCHAR(255),
  tariffId                  INT REFERENCES tariffs(tariffId),
  orderId                   INT REFERENCES orders(orderId)
);
INSERT INTO tickets (qrCode, tariffId, orderId) VALUES ('145345343453543', 1, 1);
--INSERT INTO tickets (tariffId, orderId) VALUES (1, 1);

---CREATE TABLE tariffsBlocked (
---  tariffsBlockedId         SERIAL PRIMARY KEY,
---  endTime                  TIMESTAMP DEFAULT current_timestamp + time '00:15' NOT NULL,
---  tariffId                 BIGINT REFERENCES tariffs(tariffId)
---);


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
    name                    VARCHAR(255) DEFAULT 'tamere666' NOT NULL,
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
  userId                   INT REFERENCES users(userId),
  eventId                  INT REFERENCES events(eventId),
  PRIMARY KEY (userId, eventId)
);
INSERT INTO eventsFollowed (userId, eventId) VALUES (1, 1);
INSERT INTO eventsFollowed (userId, eventId) VALUES (1, 2);
INSERT INTO eventsFollowed (userId, eventId) VALUES (1, 3);
INSERT INTO eventsFollowed (userId, eventId) VALUES (2, 1);
INSERT INTO eventsFollowed (userId, eventId) VALUES (2, 3);

CREATE TABLE artistsFollowed (
  userId                   INT REFERENCES users(userId),
  artistId                 INT REFERENCES artists(artistId),
  PRIMARY KEY (userId, artistId)
);
INSERT INTO artistsFollowed (userId, artistId) VALUES (1, 1);


CREATE TABLE placesFollowed (
  userId                   INT REFERENCES users(userId),
  placeId                  INT REFERENCES places(placeId),
  PRIMARY KEY (userId, placeId)
);
INSERT INTO placesFollowed (userId, placeId) VALUES (1, 1);


CREATE TABLE usersFollowed (
  userIdFollower          INT REFERENCES users(userId),
  userIdFollowed          INT REFERENCES users(userId),
  PRIMARY KEY (userIdFollower, userIdFollowed)
);
INSERT INTO usersFollowed (userIdFollower, userIdFollowed) VALUES (1, 2);
INSERT INTO usersFollowed (userIdFollower, userIdFollowed) VALUES (2, 1);


CREATE TABLE eventsPlaces (
    eventId INT REFERENCES events (eventId),
    placeId INT REFERENCES places (placeId),
    PRIMARY KEY (eventId, placeId)
);
INSERT INTO eventsPlaces (eventId, placeId)
VALUES (1, 1);
INSERT INTO eventsPlaces (eventId, placeId)
VALUES (1, 2);
INSERT INTO eventsPlaces (eventId, placeId)
VALUES (2, 1);

CREATE TABLE eventsUsers (
    eventId INT REFERENCES events (eventId),
    userId INT REFERENCES users (userId),
    PRIMARY KEY (eventId, userId)
);
INSERT INTO eventsUsers (eventId, userId)
VALUES (1, 1);
INSERT INTO eventsUsers (eventId, userId)
VALUES (1, 2);
INSERT INTO eventsUsers (eventId, userId)
VALUES (2, 1);
INSERT INTO eventsUsers (eventId, userId)
VALUES (3, 1);
INSERT INTO eventsUsers (eventId, userId)
VALUES (4, 2);
INSERT INTO eventsUsers (eventId, userId)
VALUES (4, 1);


CREATE TABLE eventsArtists (
    eventId INT REFERENCES events (eventId),
    artistId INT REFERENCES artists (artistId),
    PRIMARY KEY (eventId, artistId)
);
INSERT INTO eventsArtists (eventId, artistId)
VALUES (1, 1);
INSERT INTO eventsArtists (eventId, artistId)
VALUES (2, 2);
INSERT INTO eventsArtists (eventId, artistId)
VALUES (3, 3);


CREATE TABLE usersArtists (
    userId INT REFERENCES users (userId),
    artistId INT REFERENCES artists (artistId),
    PRIMARY KEY (userId, artistId)
);

# --- !Downs
DROP TABLE IF EXISTS eventsPlaces;
DROP TABLE IF EXISTS eventsUsers;
DROP TABLE IF EXISTS eventsArtists;
DROP TABLE IF EXISTS usersArtists;
DROP TABLE IF EXISTS eventsFollowed;
DROP TABLE IF EXISTS artistsFollowed;
DROP TABLE IF EXISTS placesFollowed;
DROP TABLE IF EXISTS usersFollowed;
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
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS infos;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS places;
DROP TABLE IF EXISTS amountDue;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS users_login, users_token;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS addresses;


