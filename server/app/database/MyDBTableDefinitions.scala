package database

import java.sql.{JDBCType, Timestamp}
import java.util.UUID

import addresses.Address
import application.GuestUser
import artistsDomain.Artist
import attendees.{FacebookAttendee, FacebookAttendeeEventRelation}
import com.vividsolutions.jts.geom.Geometry
import database.MyPostgresDriver.api._
import eventsDomain.Event
import genresDomain.Genre
import issues.{Issue, IssueComment}
import org.joda.time.DateTime
import organizersDomain.Organizer
import placesDomain.Place
import play.api.Logger
import playlistsDomain.Playlist
import silhouette.DBTableDefinitions
import slick.jdbc.{PositionedParameters, SetParameter}
import slick.model.ForeignKeyAction
import trackingDomain.{UserAction, UserSession}
import tracksDomain.{Track, TrackRating}
import tariffsDomain.Tariff
import ticketsDomain._
import tracksDomain.{TrackRating, Track}


case class UserArtistRelation(userId: UUID, artistId: Long)
case class EventArtistRelation(eventId: Long, artistId: Long)
case class EventTariffRelation(eventId: Long, tariffId: Long)
case class EventGenreRelation(eventId: Long, genreId: Int)
case class EventPlaceRelation(eventId: Long, placeId: Long)
case class EventAddressRelation(eventId: Long, addressId: Long)
case class ArtistGenreRelation(artistId: Long, genreId: Int, weight: Int = 0)
case class UserGenreRelation(userId: UUID, genreId: Int)
case class UserEventRelation(userId: UUID, eventId: Long)
case class TrackGenreRelation(trackId: UUID, genreId: Int, weight: Int = 0)
case class UserPlaceRelation(userId: UUID, placeId: Long)
case class UserOrganizerRelation(userId: UUID, organizerId: Long)
case class UserTrackRelation(userId: UUID, trackId: UUID)
case class EventOrganizerRelation(eventId: Long, organizerId: Long)
case class FrenchCity(city: String, geographicPoint: Geometry)
case class PlaylistTrack(playlistId: Long, trackId: UUID, trackRank: Double)


trait MyDBTableDefinitions extends DBTableDefinitions {

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) }
  }

  def optionStringToSet(maybeString: Option[String]): Set[String] = maybeString match {
    case None => Set.empty
    case Some(string) => string.split(",").map(_.trim).filter(_.nonEmpty).toSet
  }

  implicit val jodaDateTimeMapping = {
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }

  class Artists(tag: Tag) extends Table[Artist](tag, "artists") {
    def id = column[Long]("artistid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def name = column[String]("name")
    def imagePath = column[Option[String]]("imagepath")
    def description = column[Option[String]]("description")
    def facebookUrl = column[String]("facebookurl")
    def websites = column[Option[String]]("websites")
    def hasTracks = column[Boolean]("hastracks", O.Default(false))

    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites, hasTracks).shaped <> (
      { case (id, facebookId, name, imagePath, description, facebookUrl, websites, hasTracks) =>
        Artist(id, facebookId, name, imagePath, description, facebookUrl, optionStringToSet(websites), hasTracks)
      }, { artist: Artist =>
      Some((artist.id, artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
        Option(artist.websites.mkString(",")), artist.hasTracks))
    })
  }

  class ArtistsFollowed(tag: Tag) extends Table[UserArtistRelation](tag, "artistsfollowed") {
    def userId = column[UUID]("userid")
    def artistId = column[Long]("artistid")

    def * = (userId, artistId) <> ((UserArtistRelation.apply _).tupled, UserArtistRelation.unapply)
  }

  class ArtistsGenres(tag: Tag) extends Table[ArtistGenreRelation](tag, "artistsgenres") {
    def artistId = column[Long]("artistid")
    def genreId = column[Int]("genreid")
    def weight = column[Int]("weight", O.Default(0))

    def * = (artistId, genreId, weight) <> ((ArtistGenreRelation.apply _).tupled, ArtistGenreRelation.unapply)

    def aFK = foreignKey("artistid", artistId, artists)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class Genres(tag: Tag) extends Table[Genre](tag, "genres") {
    def id = column[Int]("genreid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def icon = column[Char]("icon")

    def * = (id.?, name, icon) <> ((Genre.apply _).tupled, Genre.unapply)
  }

  class GenresFollowed(tag: Tag) extends Table[UserGenreRelation](tag, "genresfollowed") {
    def userId = column[UUID]("userid")
    def genreId = column[Int]("genreid")

    def * = (userId, genreId) <> ((UserGenreRelation.apply _).tupled, UserGenreRelation.unapply)
  }

  class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("eventid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def isPublic = column[Boolean]("ispublic")
    def isActive = column[Boolean]("isactive")
    def name = column[String]("name")
    def geographicPoint = column[Geometry]("geographicpoint")
    def description = column[Option[String]]("description")
    def startTime = column[DateTime]("starttime")
    def endTime = column[Option[DateTime]]("endtime")
    def ageRestriction = column[Int]("agerestriction")
    def tariffRange = column[Option[String]]("tariffrange")
    def ticketSellers = column[Option[String]]("ticketsellers")
    def imagePath = column[Option[String]]("imagepath")

    def * = (id.?, facebookId, isPublic, isActive, name, geographicPoint, description, startTime, endTime,
      ageRestriction, tariffRange, ticketSellers, imagePath) <> ((Event.apply _).tupled, Event.unapply)
  }

  class EventsFollowed(tag: Tag) extends Table[UserEventRelation](tag, "eventsfollowed") {
    def userId = column[UUID]("userid")
    def eventId = column[Long]("eventid")

    def * = (userId, eventId) <> ((UserEventRelation.apply _).tupled, UserEventRelation.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsPlaces(tag: Tag) extends Table[EventPlaceRelation](tag, "eventsplaces") {
    def eventId = column[Long]("eventid")
    def placeId = column[Long]("placeid")

    def * = (eventId, placeId) <> ((EventPlaceRelation.apply _).tupled, EventPlaceRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("placeid", placeId, places)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsAddresses(tag: Tag) extends Table[EventAddressRelation](tag, "eventsaddresses") {
    def eventId = column[Long]("eventid")
    def addressId = column[Long]("addressid")

    def * = (eventId, addressId) <> ((EventAddressRelation.apply _).tupled, EventAddressRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("addressid", addressId, addresses)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsGenres(tag: Tag) extends Table[EventGenreRelation](tag, "eventsgenres") {
    def eventId = column[Long]("eventid")
    def genreId = column[Int]("genreid")

    def * = (eventId, genreId) <> ((EventGenreRelation.apply _).tupled, EventGenreRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsOrganizers(tag: Tag) extends Table[EventOrganizerRelation](tag, "eventsorganizers") {
    def eventId = column[Long]("eventid")
    def organizerId = column[Long]("organizerid")

    def * = (eventId, organizerId) <> ((EventOrganizerRelation.apply _).tupled, EventOrganizerRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("organizerid", organizerId, organizers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsArtists(tag: Tag) extends Table[EventArtistRelation](tag, "eventsartists") {
    def eventId = column[Long]("eventid")
    def artistId = column[Long]("artistid")

    def * = (eventId, artistId) <> ((EventArtistRelation.apply _).tupled, EventArtistRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("artistid", artistId, artists)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class Places(tag: Tag) extends Table[Place](tag, "places") {
    def id = column[Long]("placeid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def facebookId = column[Option[String]]("facebookid")
    def geographicPoint = column[Geometry]("geographicpoint")
    def description = column[Option[String]]("description")
    def websites = column[Option[String]]("websites")
    def capacity = column[Option[Int]]("capacity")
    def openingHours = column[Option[String]]("openinghours")
    def imagePath = column[Option[String]]("imagepath")
    def addressId = column[Option[Long]]("addressid")
    def linkedOrganizerId = column[Option[Long]]("linkedorganizerid")

    def * = (id.?, name, facebookId, geographicPoint, description, websites, capacity, openingHours,
      imagePath, addressId, linkedOrganizerId) <> ((Place.apply _).tupled, Place.unapply)
  }

  class PlacesFollowed(tag: Tag) extends Table[UserPlaceRelation](tag, "placesfollowed") {
    def userId = column[UUID]("userid")
    def placeId = column[Long]("placeid")

    def * = (userId, placeId) <> ((UserPlaceRelation.apply _).tupled, UserPlaceRelation.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("placeid", placeId, places)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class Organizers(tag: Tag) extends Table[Organizer](tag, "organizers") {
    def id = column[Long]("organizerid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def addressId = column[Option[Long]]("addressid")
    def phone = column[Option[String]]("phone")
    def publicTransit = column[Option[String]]("publictransit")
    def websites = column[Option[String]]("websites")
    def verified = column[Boolean]("verified")
    def imagePath = column[Option[String]]("imagepath")
    def geographicPoint = column[Geometry]("geographicpoint")
    def linkedPlaceId = column[Option[Long]]("placeid")

    def * = (id.?, facebookId, name, description, addressId, phone, publicTransit, websites, verified, imagePath,
      geographicPoint, linkedPlaceId) <> ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  class OrganizersFollowed(tag: Tag) extends Table[UserOrganizerRelation](tag, "organizersfollowed") {
    def userId = column[UUID]("userid")
    def organizerId = column[Long]("organizerid")

    def * = (userId, organizerId) <> ((UserOrganizerRelation.apply _).tupled, UserOrganizerRelation.unapply)
  }

  class Tracks(tag: Tag) extends Table[Track](tag, "tracks") {
    def uuid = column[UUID]("trackid", O.PrimaryKey)
    def url = column[String]("url")
    def title = column[String]("title")
    def platform = column[Char]("platform")
    def thumbnailUrl = column[String]("thumbnailurl")
    def artistFacebookUrl = column[String]("artistfacebookurl")
    def artistName = column[String]("artistname")
    def redirectUrl = column[Option[String]]("redirecturl")
    def confidence = column[Double]("confidence", O.Default(0.0))
    def ratingUp = column[Int]("ratingup")
    def ratingDown = column[Int]("ratingdown")

    def * = (uuid, title, url, platform, thumbnailUrl, artistFacebookUrl, artistName, redirectUrl, confidence) <>
      ((Track.apply _).tupled, Track.unapply)

    def aFK = foreignKey("artistfacebookurl", artistFacebookUrl, artists)(_.facebookUrl, onDelete = ForeignKeyAction.Cascade)
  }

  class TrackRatings(tag: Tag) extends Table[TrackRating](tag, "tracksrating") {
    def tableId = column[Long]("tableid", O.PrimaryKey, O.AutoInc)
    def userId = column[UUID]("userid")
    def trackId = column[UUID]("trackid")
    def ratingUp = column[Int]("ratingup", O.Default(0))
    def ratingDown = column[Int]("ratingdown", O.Default(0))
    def reason = column[Option[Char]]("reason")

    def * = (userId, trackId, ratingUp, ratingDown, reason) <> ((TrackRating.apply _).tupled, TrackRating.unapply)
  }

  class TracksGenres(tag: Tag) extends Table[TrackGenreRelation](tag, "tracksgenres") {
    def trackId = column[UUID]("trackid")
    def genreId = column[Int]("genreid")
    def weight = column[Int]("weight", O.Default(0))

    def * = (trackId, genreId, weight) <> ((TrackGenreRelation.apply _).tupled, TrackGenreRelation.unapply)
  }

  class TracksFollowed(tag: Tag) extends Table[UserTrackRelation](tag, "tracksfollowed") {
    def userId = column[UUID]("userid")
    def trackId = column[UUID]("trackid")

    def * = (userId, trackId) <> ((UserTrackRelation.apply _).tupled, UserTrackRelation.unapply)
  }

  class Addresses(tag: Tag) extends Table[Address](tag, "addresses") {
    def id = column[Long]("addressid", O.PrimaryKey, O.AutoInc)
    def geographicPoint = column[Geometry]("geographicpoint")
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")

    def * = (id.?, geographicPoint, city, zip, street) <> ((Address.apply _).tupled, Address.unapply)
  }

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def id = column[Long]("cityid", O.PrimaryKey, O.AutoInc)
    def city = column[String]("city")
    def geographicPoint = column[Geometry]("geographicpoint")

    def * = (city, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
  }

  class Playlists(tag: Tag) extends Table[Playlist](tag, "playlists") {
    def id = column[Long]("playlistid", O.PrimaryKey, O.AutoInc)
    def userId = column[UUID]("userid")
    def name = column[String]("name")

    def * = (id.?, userId, name) <> ((Playlist.apply _).tupled, Playlist.unapply)

    def aFK = foreignKey("userId", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class PlaylistsTracks(tag: Tag) extends Table[PlaylistTrack](tag, "playliststracks") {
    def playlistId = column[Long]("playlistid")
    def trackId = column[UUID]("trackid")
    def trackRank = column[Double]("trackrank")

    def * = (playlistId, trackId, trackRank) <> ((PlaylistTrack.apply _).tupled, PlaylistTrack.unapply)

    def aFK = foreignKey("playlistId", playlistId, playlists)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("trackId", trackId, tracks)(_.uuid, onDelete = ForeignKeyAction.Cascade)
  }

  class Issues(tag: Tag) extends Table[Issue](tag, "issues") {
    def id = column[Long]("issueid", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def content = column[String]("content")
    def userUUID = column[UUID]("userid")
    def fixed = column[Boolean]("fixed")

    def * = (id.?, title, content, userUUID, fixed) <> ((Issue.apply _).tupled, Issue.unapply)

    def aFK = foreignKey("userid", userUUID, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class IssuesComments(tag: Tag) extends Table[IssueComment](tag, "issuescomments") {
    def content = column[String]("content")
    def userId = column[UUID]("userid")
    def issueId = column[Long]("issueid")

    def * = (content, userId, issueId) <>((IssueComment.apply _).tupled, IssueComment.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("issueid", issueId, issues)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class FacebookAttendees(tag: Tag) extends Table[FacebookAttendee](tag, "facebookattendees") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def attendeeFacebookId = column[String]("attendeefacebookid")
    def name = column[String]("name")

    def * = (attendeeFacebookId, name) <> ((FacebookAttendee.apply _).tupled, FacebookAttendee.unapply)
  }
  lazy val facebookAttendees = TableQuery[FacebookAttendees]

  class FacebookAttendeeEventRelations(tag: Tag) extends Table[FacebookAttendeeEventRelation](tag, "facebookattendeeeventrelations") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def attendeeFacebookId = column[String]("attendeefacebookid")
    def eventFacebookId = column[String]("eventfacebookid")
    def attendeeStatus = column[Char]("attendeestatus")

    def * = (attendeeFacebookId, eventFacebookId, attendeeStatus) <>
      ((FacebookAttendeeEventRelation.apply _).tupled, FacebookAttendeeEventRelation.unapply)

    def aFK = foreignKey("attendeefacebookid", attendeeFacebookId, facebookAttendees)(_.attendeeFacebookId, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("eventfacebookid", eventFacebookId, events)(_.facebookId.getOrElse{Logger.error("Attendee not linked to an event"); "0"},
      onDelete = ForeignKeyAction.Cascade)
  }
  lazy val facebookAttendeeEventRelations = TableQuery[FacebookAttendeeEventRelations]

  class Tickets(tag: Tag) extends Table[Ticket](tag, "tickets") {
    def ticketId = column[Long]("ticketid", O.PrimaryKey, O.AutoInc)
    def qrCode = column[String]("qrcode")
    def eventId = column[Long]("eventid")
    def tariffId = column[Long]("tariffid")

    def * = (ticketId.?, qrCode, eventId, tariffId) <> ((Ticket.apply _).tupled, Ticket.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id)
    def bFK = foreignKey("tariffid", tariffId, tariffs)(_.tariffId)
  }

  class TicketStatuses(tag: Tag) extends Table[TicketStatus](tag, "ticketstatuses") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ticketId = column[Long]("ticketid")
    def status = column[Char]("status")
    def date = column[DateTime]("date")

    def * = (ticketId, status, date) <> ((TicketStatus.apply _).tupled, TicketStatus.unapply)

    def aFK = foreignKey("ticketid", ticketId, tickets)(_.ticketId)
  }

  class BlockedTickets(tag: Tag) extends Table[BlockedTicket](tag, "blockedtickets") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ticketId = column[Long]("ticketid")
    def expirationDate = column[DateTime]("expirationdate")
    def userId = column[UUID]("userid")

    def * = (ticketId, expirationDate, userId) <> ((BlockedTicket.apply _).tupled, BlockedTicket.unapply)

    def aFK = foreignKey("ticketid", ticketId, tickets)(_.ticketId)
    def bFK = foreignKey("userid", userId, slickUsers)(_.id)
  }

  class Tariffs(tag: Tag) extends Table[Tariff](tag, "tariffs") {
    def tariffId = column[Long]("tariffid", O.PrimaryKey, O.AutoInc)
    def denomination = column[String]("denomination")
    def eventId = column[Long]("eventid")
    def startTime = column[DateTime]("starttime")
    def endTime = column[DateTime]("endtime")
    def price = column[BigDecimal]("price")

    def * = (tariffId.?, denomination, eventId, startTime, endTime, price) <> ((Tariff.apply _).tupled, Tariff.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id)
  }

  class BoughtTicketBills(tag: Tag) extends Table[TicketBill](tag, "boughtticketbills") {
    def billId = column[Long]("billid", O.PrimaryKey, O.AutoInc)
    def ticketId = column[Long]("ticketid")
    def userId = column[UUID]("userid")
    def date = column[DateTime]("date")
    def amount = column[BigDecimal]("amount")

    def * = (ticketId, userId, date, amount) <> ((TicketBill.apply _).tupled, TicketBill.unapply)

    def aFK = foreignKey("ticketid", ticketId, tickets)(_.ticketId)
    def bFK = foreignKey("userid", userId, slickUsers)(_.id)
  }

  class SoldTicketBills(tag: Tag) extends Table[TicketBill](tag, "soldticketbills") {
    def billId = column[Long]("billid", O.PrimaryKey, O.AutoInc)
    def ticketId = column[Long]("ticketid")
    def userId = column[UUID]("userid")
    def date = column[DateTime]("date")
    def amount = column[BigDecimal]("amount")

    def * = (ticketId, userId, date, amount) <> ((TicketBill.apply _).tupled, TicketBill.unapply)

    def aFK = foreignKey("ticketid", ticketId, tickets)(_.ticketId)
    def bFK = foreignKey("userid", userId, slickUsers)(_.id)
  }

  class PendingTickets(tag: Tag) extends Table[PendingTicket](tag, "pendingtickets") {
    def pendingTicketId = column[Long]("pendingticketid", O.PrimaryKey, O.AutoInc)
    def userId = column[UUID]("userid")
    def tariffId = column[Long]("tariffid")
    def date = column[DateTime]("date")
    def amount = column[BigDecimal]("amount")
    def qrCode = column[String]("qrcode")
    def isValidated = column[Option[Boolean]]("isvalidated")

    def * = (pendingTicketId.?, userId, tariffId, date, amount, qrCode, isValidated) <> ((PendingTicket.apply _).tupled, PendingTicket.unapply)

    def aFK = foreignKey("tariffid", tariffId, tariffs)(_.tariffId)
    def bFK = foreignKey("userid", userId, slickUsers)(_.id)
  }

  class SalableEvents(tag: Tag) extends Table[SalableEvent](tag, "salableevents") {
    def eventId = column[Long]("eventid")

    def * = eventId <> (SalableEvent.apply, SalableEvent.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id)
  }
  lazy val salableEvents = TableQuery[SalableEvents]

  class GuestUsers(tag: Tag) extends Table[GuestUser](tag, "guestusers") {
    def ip = column[String]("ip", O.PrimaryKey)
    def userUuid = column[UUID]("useruuid")

    def * = (ip, userUuid.?) <> ((GuestUser.apply _).tupled, GuestUser.unapply)

    def aFK = foreignKey("useruuid", userUuid, slickUsers)(_.id)
  }
  lazy val guestUsers = TableQuery[GuestUsers]

  class UserSessions(tag: Tag) extends Table[UserSession](tag, "usersessions") {
    def id = column[UUID]("id", O.PrimaryKey)
    def ip = column[String]("ip")

    def * = (id, ip) <> ((UserSession.apply _).tupled, UserSession.unapply)

    def aFK = foreignKey("ip", ip, guestUsers)(_.ip)
  }
  lazy val userSessions = TableQuery[UserSessions]

  class UserActions(tag: Tag) extends Table[UserAction](tag, "useractions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def action = column[String]("action")
    def timestamp = column[Timestamp]("timestamp")
    def sessionId = column[UUID]("sessionid")

    def * = (action, timestamp, sessionId) <> ((UserAction.apply _).tupled, UserAction.unapply)

    def aFK = foreignKey("sessionid", sessionId, userSessions)(_.id)
  }
  lazy val userActions = TableQuery[UserActions]


  lazy val artistsFollowed = TableQuery[ArtistsFollowed]
  lazy val genres = TableQuery[Genres]
  lazy val genresFollowed = TableQuery[GenresFollowed]
  lazy val artistsGenres = TableQuery[ArtistsGenres]
  lazy val artists = TableQuery[Artists]
  lazy val events = TableQuery[Events]
  lazy val eventsAddresses = TableQuery[EventsAddresses]
  lazy val eventsOrganizers = TableQuery[EventsOrganizers]
  lazy val eventsArtists = TableQuery[EventsArtists]
  lazy val eventsGenres = TableQuery[EventsGenres]
  lazy val eventsPlaces = TableQuery[EventsPlaces]
  lazy val eventsFollowed = TableQuery[EventsFollowed]
  lazy val places = TableQuery[Places]
  lazy val placesFollowed = TableQuery[PlacesFollowed]
  lazy val organizers = TableQuery[Organizers]
  lazy val trackRatings = TableQuery[TrackRatings]
  lazy val organizersFollowed = TableQuery[OrganizersFollowed]
  lazy val addresses = TableQuery[Addresses]
  lazy val frenchCities = TableQuery[FrenchCities]
  lazy val tracks = TableQuery[Tracks]
  lazy val tracksGenres = TableQuery[TracksGenres]
  lazy val tracksFollowed = TableQuery[TracksFollowed]
  lazy val playlists = TableQuery[Playlists]
  lazy val playlistsTracks = TableQuery[PlaylistsTracks]
  lazy val issues = TableQuery[Issues]
  lazy val issuesComments = TableQuery[IssuesComments]
  lazy val tickets = TableQuery[Tickets]
  lazy val ticketStatuses = TableQuery[TicketStatuses]
  lazy val boughtTicketBills = TableQuery[BoughtTicketBills]
  lazy val pendingTickets = TableQuery[PendingTickets]
  lazy val soldTicketBills = TableQuery[SoldTicketBills]
  lazy val blockedTickets = TableQuery[BlockedTickets]
  lazy val tariffs = TableQuery[Tariffs]
}
