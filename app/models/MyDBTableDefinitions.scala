package models

import java.sql.{JDBCType, Timestamp}
import java.util.UUID

import com.vividsolutions.jts.geom.Geometry
import org.joda.time.DateTime
import services.MyPostgresDriver.api._
import silhouette.DBTableDefinitions
import slick.jdbc.{PositionedParameters, SetParameter}
import slick.model.ForeignKeyAction

case class UserArtistRelation(userId: UUID, artistId: Long)
case class EventArtistRelation(eventId: Long, artistId: Long)
case class ArtistGenreRelation(artistId: Long, genreId: Int, weight: Long = 0)
case class UserGenreRelation(userId: UUID, genreId: Long)
case class UserEventRelation(userId: UUID, eventId: Long)
case class TrackGenreRelation(trackId: UUID, genreId: Int, weight: Long = 0)
case class UserPlaceRelation(userId: UUID, placeId: Long)
case class EventGenreRelation(eventId: Long, genreId: Int)
case class EventPlaceRelation(eventId: Long, placeId: Long)
case class EventOrganizerRelation(eventId: Long, organizerId: Long)
case class FrenchCity(city: String, geographicPoint: Geometry)


trait MyDBTableDefinitions extends DBTableDefinitions {

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) { pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber) }
  }

  def optionStringToSet(maybeString: Option[String]): Set[String] = maybeString match {
    case None => Set.empty
    case Some(string) => string.split(",").filter(_.nonEmpty).toSet
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

    def * = (id.?, facebookId, name, imagePath, description, facebookUrl, websites).shaped <> (
      { case (id, facebookId, name, imagePath, description, facebookUrl, websites) =>
        Artist(id, facebookId, name, imagePath, description, facebookUrl, optionStringToSet(websites))
      }, { artist: Artist =>
      Some((artist.id, artist.facebookId, artist.name, artist.imagePath, artist.description, artist.facebookUrl,
        Option(artist.websites.mkString(","))))
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
    def weight = column[Long]("weight", O.Default(0))

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
    def genreId = column[Long]("genreid")

    def * = (userId, genreId) <> ((UserGenreRelation.apply _).tupled, UserGenreRelation.unapply)
  }

  class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("eventid", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookid")
    def isPublic = column[Boolean]("ispublic")
    def isActive = column[Boolean]("isactive")
    def name = column[String]("name")
    def geographicPoint = column[Option[Geometry]]("geographicpoint")
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

  class EventsAddresses(tag: Tag) extends Table[(Long, Long)](tag, "eventsaddresses") {
    def eventId = column[Long]("eventid")
    def addressId = column[Long]("addressid")

    def * = (eventId, addressId) //<> ((EventAddresss.apply _).tupled, EventAddresss.unapply)

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
    def geographicPoint = column[Option[Geometry]]("geographicpoint")
    def description = column[Option[String]]("description")
    def websites = column[Option[String]]("websites")
    def capacity = column[Option[Int]]("capacity")
    def openingHours = column[Option[String]]("openinghours")
    def imagePath = column[Option[String]]("imagepath")
    def linkedOrganizerId = column[Option[Long]]("linkedorganizerid")

    def * = (id.?, name, facebookId, geographicPoint, description, websites, capacity, openingHours,
      imagePath, linkedOrganizerId) <> ((Place.apply _).tupled, Place.unapply)
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
    def geographicPoint = column[Option[Geometry]]("geographicpoint")
    def linkedPlaceId = column[Option[Long]]("placeid")

    def * = (id.?, facebookId, name, description, addressId, phone, publicTransit, websites, verified, imagePath, geographicPoint, linkedPlaceId) <>
      ((Organizer.apply _).tupled, Organizer.unapply)

    def address = foreignKey("addressFk", addressId, addresses)(_.id.?, onDelete = ForeignKeyAction.Cascade)
  }

  class OrganizersFollowed(tag: Tag) extends Table[OrganizerFollowed](tag, "organizersfollowed") {
    def userId = column[UUID]("userid")
    def organizerId = column[Long]("organizerid")

    def * = (userId, organizerId) <> ((OrganizerFollowed.apply _).tupled, OrganizerFollowed.unapply)
  }

  class Tracks(tag: Tag) extends Table[Track](tag, "tracks") {
    /*def id = column[Long]("trackid", O.PrimaryKey, O.AutoInc)*/
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

  class TracksGenres(tag: Tag) extends Table[TrackGenreRelation](tag, "tracksgenres") {
    def trackId = column[UUID]("trackid")
    def genreId = column[Int]("genreid")
    def weight = column[Long]("weight", O.Default(0))

    def * = (trackId, genreId, weight) <> ((TrackGenreRelation.apply _).tupled, TrackGenreRelation.unapply)
  }

  class Addresses(tag: Tag) extends Table[Address](tag, "addresses") {
    def id = column[Long]("addressid", O.PrimaryKey, O.AutoInc)
    def geographicPoint = column[Option[Geometry]]("geographicpoint")
    def city = column[Option[String]]("city")
    def zip = column[Option[String]]("zip")
    def street = column[Option[String]]("street")
    def * = (id.?, geographicPoint, city, zip, street) <>
      ((Address.apply _).tupled, Address.unapply)
  }

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def id = column[Long]("cityid", O.PrimaryKey, O.AutoInc)
    def city = column[String]("city")
    def geographicPoint = column[Geometry]("geographicpoint")
    def * = (city, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
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
