package models

import java.sql.Timestamp
import java.util.UUID

import com.vividsolutions.jts.geom.Point
import org.joda.time.DateTime
import services.MyPostgresDriver.api._
import services.Utilities
import silhouette.DBTableDefinitions
import slick.model.ForeignKeyAction

trait MyDBTableDefinitions extends DBTableDefinitions {
  protected val utilities: Utilities

  implicit val jodaDateTimeMapping = {
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }

  class Artists(tag: Tag) extends Table[Artist](tag, "artists") {
    def id = column[Long]("organizerId", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def name = column[String]("name")
    def imagePath = column[Option[String]]("imagepath")
    def description = column[Option[String]]("description")
    def facebookUrl = column[String]("facebookurl")
    def websites = column[Option[String]]("websites")

    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites).shaped <> (
      { case (id, facebookId, name, imagePath, description, facebookUrl, websites) =>
        Artist(id, facebookId, name, imagePath, description, facebookUrl, utilities.optionStringToSet(websites))
      }, { artist: Artist =>
      Some((artist.id, artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
        Option(artist.websites.mkString(","))))
    })
    //    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites, likes, country) <>
    //      ((Artist.apply _).tupled, Artist.unapply)
  }

  case class UserArtistRelation(userId: String, artistId: Long)

  class ArtistsFollowed(tag: Tag) extends Table[UserArtistRelation](tag, "artistsfollowed") {
    def userId = column[String]("userid")
    def artistId = column[Long]("artistid")

    def * = (userId, artistId) <> ((UserArtistRelation.apply _).tupled, UserArtistRelation.unapply)
  }

  case class ArtistGenreRelation(artistId: Long, genreId: Int)

  class ArtistsGenres(tag: Tag) extends Table[ArtistGenreRelation](tag, "artistsGenres") {
    def artistId = column[Long]("artistid")
    def genreId = column[Int]("genreid")

    def * = (artistId, genreId) <> ((ArtistGenreRelation.apply _).tupled, ArtistGenreRelation.unapply)

    def aFK = foreignKey("artistid", artistId, artists)(_.id, onDelete=ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete=ForeignKeyAction.Cascade)
  }

  class Genres(tag: Tag) extends Table[Genre](tag, "genres") {
    def id = column[Int]("genreid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def icon = column[Char]("icon")

    def * = (id.?, name, icon) <> ((Genre.apply _).tupled, Genre.unapply)
  }

  case class UserGenreRelation(userId: String, genreId: Long)

  class GenresFollowed(tag: Tag) extends Table[UserGenreRelation](tag, "genresfollowed") {
    def userId = column[String]("userid")
    def genreId = column[Long]("genreid")

    def * = (userId, genreId) <> ((UserGenreRelation.apply _).tupled, UserGenreRelation.unapply)
  }

  class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("organizerId", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def isPublic = column[Boolean]("ispublic")
    def isActive = column[Boolean]("isactive")
    def name = column[String]("name")
    def geographicPoint = column[Option[Point]]("geographicpoint")
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

  case class UserEventRelation(userId: String, eventId: Long)

  class EventsFollowed(tag: Tag) extends Table[UserEventRelation](tag, "eventsfollowed") {
    def userId = column[String]("userid")
    def eventId = column[Long]("eventid")

    def * = (userId, eventId) <> ((UserEventRelation.apply _).tupled, UserEventRelation.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class EventPlaceRelation(eventId: Long, placeId: Long)

  class EventsPlaces(tag: Tag) extends Table[EventPlaceRelation](tag, "eventsPlaces") {
    def eventId = column[Long]("eventid")
    def placeId = column[Long]("placeid")

    def * = (eventId, placeId) <> ((EventPlaceRelation.apply _).tupled, EventPlaceRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("placeid", placeId, places)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class EventsAddresses(tag: Tag) extends Table[(Long, Long)](tag, "eventsaddresses") {
    def eventId = column[Long]("eventid")
    def addressId = column[Long]("addressid")

    def * = (eventId, addressId) //<> ((EventAddresss.apply _).tupled, EventAddresss.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("addressid", addressId, addresses)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class EventGenreRelation(eventId: Long, genreId: Int)

  class EventsGenres(tag: Tag) extends Table[EventGenreRelation](tag, "eventsgenres") {
    def eventId = column[Long]("eventid")
    def genreId = column[Int]("genreid")

    def * = (eventId, genreId) <> ((EventGenreRelation.apply _).tupled, EventGenreRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class EventOrganizer(eventId: Long, organizerId: Long)

  class EventsOrganizers(tag: Tag) extends Table[EventOrganizer](tag, "eventsorganizers") {
    def eventId = column[Long]("eventid")
    def organizerId = column[Long]("organizerid")

    def * = (eventId, organizerId) <> ((EventOrganizer.apply _).tupled, EventOrganizer.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("organizerid", organizerId, organizers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class EventArtistRelation(eventId: Long, artistId: Long)

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
    def geographicPoint = column[Option[Point]]("geographicpoint")
    def description = column[Option[String]]("description")
    def websites = column[Option[String]]("websites")
    def capacity = column[Option[Int]]("capacity")
    def openingHours = column[Option[String]]("openinghours")
    def imagePath = column[Option[String]]("imagepath")
    def linkedOrganizerId = column[Option[Long]]("linkedorganizerid")

    def * = (id.?, name, facebookId, geographicPoint, description, websites, capacity, openingHours,
      imagePath, linkedOrganizerId) <> ((Place.apply _).tupled, Place.unapply)
  }

  case class UserPlaceRelation(userId: String, placeId: Long)

  class PlacesFollowed(tag: Tag) extends Table[UserPlaceRelation](tag, "placesfollowed") {
    def userId = column[String]("userid")
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
    def geographicPoint = column[Option[Point]]("geographicpoint")
    def linkedPlaceId = column[Option[Long]]("placeid")

    def * = (id.?, facebookId, name, description, addressId, phone, publicTransit, websites, verified, imagePath, geographicPoint, linkedPlaceId) <>
      ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  class OrganizersFollowed(tag: Tag) extends Table[OrganizerFollowed](tag, "organizersfollowed") {
    def userId = column[String]("userid")
    def organizerId = column[Long]("organizerid")

    def * = (userId, organizerId) <> ((OrganizerFollowed.apply _).tupled, OrganizerFollowed.unapply)
  }

  class Addresses(tag: Tag) extends Table[Address](tag, "addresses") {
    def id = column[Long]("organizerId", O.PrimaryKey)
    def geographicPoint = column[Option[String]]("geographicPoint")
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")
    def * = (id.?, geographicPoint, city, zip, street) <>
      ((Address.apply _).tupled, Address.unapply)
  }

  case class FrenchCity(city: String, geographicPoint: Point)

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def id = column[Long]("cityid", O.PrimaryKey)
    def city = column[String]("city")
    def geographicPoint = column[Point]("geographicpoint")
    def * = (city, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
  }

  class Tracks(tag: Tag) extends Table[Track](tag, "tracks") {
    def id = column[Long]("organizerid", O.PrimaryKey, O.AutoInc)
    def uuid = column[UUID]("uuid")
    def url = column[String]("url")
    def title = column[String]("title")
    def platform = column[Char]("platform")
    def thumbnailUrl = column[String]("thumbnailurl")
    def artistFacebookUrl = column[String]("artistfacebookurl")
    def artistName = column[String]("artistname")
    def redirectUrl = column[Option[String]]("redirecturl")
    def confidence = column[Option[Double]]("confidence")
    def playlistRank = column[Option[Double]]("playlistrank")
    def ratingUp = column[Int]("ratingup")
    def ratingDown = column[Int]("ratingdown")

    def * = (uuid, url, title, platform, thumbnailUrl, artistFacebookUrl, artistName, redirectUrl, confidence,
      playlistRank) <> ((Track.apply _).tupled, Track.unapply)
  }

  case class TrackGenreRelation(trackId: UUID, genreId: Int)

  class TracksGenres(tag: Tag) extends Table[TrackGenreRelation](tag, "tracksgenres") {
    def trackId = column[UUID]("trackid")
    def genreId = column[Int]("genreid")
    def weight = column[Long]("weight")

    def * = (trackId, genreId) <> ((TrackGenreRelation.apply _).tupled, TrackGenreRelation.unapply)
  }

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
  lazy val organizersFollowed = TableQuery[OrganizersFollowed]
  lazy val addresses = TableQuery[Addresses]
  lazy val frenchCities = TableQuery[FrenchCities]
  lazy val tracks = TableQuery[Tracks]
  lazy val tracksGenres = TableQuery[TracksGenres]
}