package models

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import controllers.DAOException
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.{MyPostgresDriver, Utilities}
import com.vividsolutions.jts.geom.{GeometryFactory, Point}
import silhouette.DBTableDefinitions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try
import services.MyPostgresDriver.api._
import json.JsonHelper._

case class Event(id: Option[Long],
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 name: String,
                 geographicPoint: Option[Point],
                 description: Option[String],
                 startTime: DateTime,
                 endTime: Option[DateTime],
                 ageRestriction: Int,
                 tariffRange: Option[String],
                 ticketSellers: Option[String],
                 imagePath: Option[String])/*,
                 organizers: List[OrganizerWithAddress],
                 artists: List[Artist],
                 tariffs: List[Tariff],
                 addresses: List[Address],
                 places: List[Place] = List.empty,
                 genres: Seq[Genre] = Seq.empty)*/


class EventMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val placeMethods: PlaceMethods,
                             val artistMethods: ArtistMethods,
                             val genreMethods: GenreMethods,
                             val tariffMethods: TariffMethods,
                             val addressMethods: AddressMethods,
                             val userMethods: UserMethods,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with DBTableDefinitions {

  val places = placeMethods.places
  val organizers = organizerMethods.organizers
  val artists = artistMethods.artists
  val genres = genreMethods.genres
  val addresses = addressMethods.addresses
  val geometryFactory = new GeometryFactory()

  implicit val jodaDateTimeMapping = {
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
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

  lazy val events = TableQuery[Events]

  case class UserEventRelation(userId: String, eventId: Long)

  class EventsFollowed(tag: Tag) extends Table[UserEventRelation](tag, "eventsfollowed") {
    def userId = column[String]("userid")
    def eventId = column[Long]("eventid")

    def * = (userId, eventId) <> ((UserEventRelation.apply _).tupled, UserEventRelation.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsFollowed = TableQuery[EventsFollowed]

  case class EventPlaces(userId: String, eventId: Long)

  class EventsPlaces(tag: Tag) extends Table[(Long, Long)](tag, "eventsPlaces") {
    def eventId = column[Long]("eventid")
    def placeId = column[Long]("placeid")

    def * = (eventId, placeId) //<> ((EventPlaces.apply _).tupled, EventPlaces.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("placeid", placeId, places)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsPlaces = TableQuery[EventsPlaces]  
  
   class EventsAddresses(tag: Tag) extends Table[(Long, Long)](tag, "eventsaddresses") {
    def eventId = column[Long]("eventid")
    def addressId = column[Long]("addressid")

    def * = (eventId, addressId) //<> ((EventAddresss.apply _).tupled, EventAddresss.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("addressid", addressId, addresses)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsAddresses = TableQuery[EventsAddresses]

  case class EventGenreRelation(eventId: Long, genreId: Int)
  
  class EventsGenres(tag: Tag) extends Table[EventGenreRelation](tag, "eventsgenres") {
    def eventId = column[Long]("eventid")
    def genreId = column[Int]("genreid")

    def * = (eventId, genreId) <> ((EventGenreRelation.apply _).tupled, EventGenreRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("genreid", genreId, genres)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsGenres = TableQuery[EventsGenres]

  case class EventOrganizer(eventId: Long, organizerId: Long)

  class EventsOrganizers(tag: Tag) extends Table[EventOrganizer](tag, "eventsorganizers") {
    def eventId = column[Long]("eventid")
    def organizerId = column[Long]("organizerid")

    def * = (eventId, organizerId) <> ((EventOrganizer.apply _).tupled, EventOrganizer.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("organizerid", organizerId, organizers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsOrganizers = TableQuery[EventsOrganizers]

  case class EventArtistRelation(eventId: Long, artistId: Long)

  class EventsArtists(tag: Tag) extends Table[EventArtistRelation](tag, "eventsartists") {
    def eventId = column[Long]("eventid")
    def artistId = column[Long]("artistid")

    def * = (eventId, artistId) <> ((EventArtistRelation.apply _).tupled, EventArtistRelation.unapply)

    def aFK = foreignKey("eventid", eventId, events)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("artistid", artistId, artists)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val eventsArtists = TableQuery[EventsArtists]

//  def formApply(name: String, geographicPoint: Option[String], description: Option[String], startTime: DateTime,
//                endTime: Option[DateTime], ageRestriction: Int, tariffRange: Option[String], ticketSellers: Option[String],
//                imagePath: Option[String], tariffs: List[Tariff], addresses: List[Address]): Event =
//    new Event(None, None, true, true, name, geographicPoint, description, startTime, endTime, ageRestriction,
//      tariffRange, ticketSellers, imagePath)//, List.empty, List.empty, tariffs, addresses)
//
//  def formUnapply(event: Event) = {
//    Some((event.name, event.geographicPoint, event.description, event.startTime, event.endTime, event.ageRestriction,
//      event.tariffRange, event.ticketSellers, event.imagePath, event.tariffs, event.addresses))
//  }

//  def getPropertiesOfEvent(event: Event): Event = event.eventId match {
//    case None => throw new DAOException("Event.getPropertiesOfEvent: event without id has been found")
//    case Some(eventId) => event.copy(
////      organizers = organizerMethods.findAllByEvent(event),
//      artists = Artist.findAllByEvent(event),
//      tariffs = Tariff.findAllByEvent(event),
//      places = placeMethods.findAllByEvent(eventId),
//      genres = Genre.findAllByEvent(eventId),
//      addresses = Address.findAllByEvent(event))
//  }

  def find(id: Long): Future[Option[Event]] = {
    val query = events.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Point, numberToReturn: Int, offset: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val query = events
      .filter(event =>
        (event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Event]] =
    addressMethods.findGeographicPointOfCity(city) flatMap {
      case None => Future { Seq.empty }
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }

  def findInPeriodNear(hourInterval: Int, geographicPoint: Point, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = events
      .filter(event =>
      (event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

    def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
      val now = DateTime.now()
      val xHoursAgo = now.minusHours(hourInterval)

      val query = events
        .filter(event => (event.startTime < now) || (event.startTime > xHoursAgo))
        .sortBy(_.geographicPoint <-> geographicPoint)
        .drop(numberToReturn)
        .take(offset)
      db.run(query.result)
    }

  def findAllByGenre(genreName: String, geographicPoint: Point, offset: Int, numberToReturn: Int): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      genre <- genres if genre.name === genreName
      eventGenre <- eventsGenres
      event <- events if event.id === eventGenre.eventId &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset).result)
  }

  def findAllByPlace(placeId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces
      event <- events if event.id === eventPlace.eventId &&
        ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }


  def findAllPassedByPlace(placeId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      place <- places if place.id === placeId
      eventPlace <- eventsPlaces
      event <- events if event.id === eventPlace.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }
  
    def findAllByOrganizer(organizerId: Long): Future[Seq[Event]] = {
      val now = DateTime.now()
      val twelveHoursAgo = now.minusHours(12)

      val query = for {
        organizer <- organizers if organizer.id === organizerId
        eventOrganizer <- eventsOrganizers
        event <- events if event.id === eventOrganizer.eventId &&
        ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
      } yield event

      //getEventProperties
      db.run(query.sortBy(_.startTime.desc).result)
    }

  def findAllPassedByOrganizer(organizerId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      organizer <- organizers if organizer.id === organizerId
      eventOrganizer <- eventsOrganizers
      event <- events if event.id === eventOrganizer.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllByArtist(facebookUrl: String): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      artist <- artists if artist.facebookUrl === facebookUrl
      eventArtist <- eventsArtists
      event <- events if event.id === eventArtist.eventId &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllPassedByArtist(artistId: Long): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = for {
      artist <- artists if artist.id === artistId
      eventArtist <- eventsArtists
      event <- events if event.id === eventArtist.eventId && event.startTime < now
    } yield event

    //getEventProperties
    db.run(query.sortBy(_.startTime.desc).result)
  }

  def findAllContaining(pattern: String): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      event <- events if (event.name.toLowerCase like s"%$lowercasePattern%") &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    db.run(query.take(20).result)
  }

  def findAllContaining(pattern: String, geographicPoint: Point): Future[Seq[Event]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      event <- events if (event.name.toLowerCase like s"%$lowercasePattern%") &&
      ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo))
    } yield event

    db.run(query.sortBy(_.geographicPoint <-> geographicPoint).take(20).result)
  }


  def findAllByCityPattern(cityPattern: String): Future[Seq[Event]] = {
//        """SELECT e.* FROM eventsAddresses eA
//          | INNER JOIN events e ON e.eventId = eA.eventId AND
//          |  (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
//          |    OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
//          | INNER JOIN addresses a ON a.addressId = eA.addressId
//          |   WHERE a.city LIKE '%'||{patternLowCase}||'%'
//          |LIMIT 50""".stripMargin)
//        .on('patternLowCase -> cityPattern.toLowerCase)
//
//    val query =
    //    .map(getPropertiesOfEvent)
    Future { Seq.empty }
  }

  def save(event: Event): Future[Event] = {
    val query = events returning events.map(_.id) into ((event, id) => event.copy(id = Some(id))) += event
    db.run(query)
  }
  /*
  def save(event: Event): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT insertEvent({facebookId}, {isPublic}, {isActive}, {name}, {geographicPoint}, {description},
           |{startTime}, {endTime}, {imagePath}, {ageRestriction}, {tariffRange}, {ticketSellers})""".stripMargin)
        .on(
          'facebookId -> event.facebookId,
          'isPublic -> event.isPublic,
          'isActive -> event.isActive,
          'name -> event.name,
          'geographicPoint -> event.geographicPoint,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime,
          'imagePath -> event.imagePath,
          'ageRestriction -> event.ageRestriction,
          'tariffRange -> event.tariffRange,
          'ticketSellers -> event.ticketSellers)
        .as(scalar[Option[Long]].single) match {
        case None => None
        case Some(eventId: Long) =>
//          event.organizers.foreach { organizer => organizerMethods.saveWithEventRelation(organizer, eventId) }
          event.tariffs.foreach { tariff => Tariff.save(tariff.copy(eventId = eventId)) }
          event.artists.foreach { artist => Artist.saveWithEventRelation(artist, eventId) }
          event.genres.foreach { genre => Genre.saveWithEventRelation(genre, eventId) }
          event.addresses.foreach { address => Address.saveAddressAndEventRelation(address, eventId) }
          Option(eventId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.save: " + e.getMessage)
  }
*/
  def delete(id: Long): Future[Int] = db.run(events.filter(_.id === id).delete)

  def update(event: Event): Future[Int] = db.run(events.filter(_.id === event.id).update(event))

  def follow(userEventRelation: UserEventRelation): Future[Int] = db.run(eventsFollowed += userEventRelation)

  def unfollow(userEventRelation: UserEventRelation): Future[Int] = db.run(
   eventsFollowed
     .filter(eventFollowed =>
        eventFollowed.userId === userEventRelation.userId && eventFollowed.eventId === userEventRelation.eventId)
     .delete)

  def getFollowedEvents(userId: String): Future[Seq[Event] ]= {
    val query = for {
      eventFollowed <- eventsFollowed if eventFollowed.userId === userId
      event <- events if event.id === eventFollowed.eventId
    } yield event

    db.run(query.result)
  }

  def isFollowed(userId: String, eventId: Long): Future[Boolean] = {
    val query = sql"""SELECT exists(SELECT 1 FROM eventsFollowed WHERE userId = $userId AND eventId = $eventId)"""
      .as[Boolean]
    db.run(query.head)
  }

  def saveFacebookEventByFacebookId(eventFacebookId: String): Future[Event] =
    findEventOnFacebookByFacebookId(eventFacebookId) flatMap { save }

  def findEventOnFacebookByFacebookId(eventFacebookId: String): Future[Event] =
    WS.url("https://graph.facebook.com/v2.2/" + eventFacebookId)
      .withQueryString(
        "fields" -> "cover,description,name,start_time,end_time,owner,venue",
        "access_token" -> utilities.facebookToken)
      .get()
      .flatMap { readFacebookEvent }

  def readFacebookEvent(eventFacebookWSResponse: WSResponse): Future[Event] = {
   val eventRead = (
     (__ \ "description").readNullable[String] and
       (__ \ "cover" \ "source").read[String] and
       (__ \ "name").read[String] and
       (__ \ "id").readNullable[String] and
       (__ \ "start_time").readNullable[String] and
       (__ \ "endTime").readNullable[String] and
       (__ \ "venue" \ "street").readNullable[String] and
       (__ \ "venue" \ "zip").readNullable[String] and
       (__ \ "venue" \ "city").readNullable[String] and
       (__ \ "owner" \ "id").readNullable[String]
     )((description: Option[String], source: String, name: String, facebookId: Option[String],
        startTime: Option[String], endTime: Option[String], street: Option[String], zip: Option[String],
        city: Option[String], maybeOwnerId: Option[String]) => {

     val eventuallyOrganizer = organizerMethods.getOrganizerInfo(maybeOwnerId)
     val address = Address(None, None, city, zip, street)

     val normalizedWebsites: Set[String] = utilities.getNormalizedWebsitesInText(description)
     val ticketSellers = tariffMethods.findTicketSellers(normalizedWebsites)
     val eventuallyMaybeArtistsFromDescription = artistMethods.getFacebookArtistsByWebsites(normalizedWebsites)
     val eventuallyMaybeArtistsFromTitle =
       artistMethods.getEventuallyArtistsInEventTitle(artistMethods.splitArtistNamesInTitle(name), normalizedWebsites)

     for {
       organizer <- eventuallyOrganizer
       artistsFromDescription <- eventuallyMaybeArtistsFromDescription
       artistsFromTitle <- eventuallyMaybeArtistsFromTitle
     } yield {

       val nonEmptyArtists = (artistsFromDescription.flatten.toList ++ artistsFromTitle).distinct
       artistMethods.saveArtistsAndTheirTracks(nonEmptyArtists)

//       val eventGenres = nonEmptyArtists.flatMap(_.genres).distinct

       Event(None, facebookId, isPublic = true, isActive = true, utilities.refactorEventOrPlaceName(name), None,
         utilities.formatDescription(description), new DateTime()/*utilities.formatDate(startTime).getOrElse(new DateTime())*/, Option(new DateTime())
         /*utilities.formatDate(endTime)*/, 16, tariffMethods.findPrices(description), ticketSellers, Option(source)/*, List(organizer).flatten,
         nonEmptyArtists, List.empty, List(address), List.empty, eventGenres*/)
     }
   })
   try {
     eventFacebookWSResponse.json.as[Future[Event]](eventRead)
   } catch {
     case e: Exception => throw new Exception("Empty event read by Event.readFacebookEvent" + e.getMessage)
   }
  }

  def getEventsFacebookIdByPlace(placeFacebookId: String): Future[Seq[String]] = {
    WS.url("https://graph.facebook.com/v2.2/" + placeFacebookId + "/events/")
      .withQueryString("access_token" -> utilities.facebookToken)
      .get()
      .map { readEventsIdsFromWSResponse }
  }

  def readEventsIdsFromWSResponse(resp: WSResponse): Seq[String] = {
    val readSoundFacebookIds: Reads[Seq[Option[String]]] = Reads.seq((__ \ "id").readNullable[String])
    (resp.json \ "data").as[Seq[Option[String]]](readSoundFacebookIds).flatten
  }
}