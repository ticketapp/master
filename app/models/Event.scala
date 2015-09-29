package models

import java.util.UUID
import javax.inject.Inject

import controllers.{DAOException, ThereIsNoOrganizerForThisFacebookIdException}
import models.Address.addresses
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.Utilities
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.model.ForeignKeyAction

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try
import org.joda.time.DateTime

case class Event(id: Option[Long],
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 name: String,
                 geographicPoint: Option[String],
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

class EventMethods @Inject()(dbConfigProvider: DatabaseConfigProvider,
                             val organizerMethods: OrganizerMethods,
                             val placeMethods: PlaceMethods,
                             val utilities: Utilities) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._

  class Events(tag: Tag) extends Table[Event](tag, "organizers") {
    def id = column[Long]("organizerId", O.PrimaryKey, O.AutoInc)
    def facebookId = column[Option[String]]("facebookId")
    def isPublic = column[Boolean]("ispublic")
    def isActive = column[Boolean]("isactive")
    def name = column[String]("name")
    def geographicPoint = column[Option[String]]("geographicpoint")
    def description = column[Option[String]]("description")
    def startTime = column[DateTime]("starttime")
    def endTime = column[Option[DateTime]]("endtime")
    def tariffRange = column[Option[String]]("tariffrange")
    def ticketSellers = column[Option[String]]("ticketsellers")
    def imagePath = column[Option[String]]("imagepath")

    def * = (id.?, facebookId, isPublic, isActive, name, geographicPoint, description, startTime, endTime, tariffRange,
      tariffRange, ticketSellers, imagePath) <> ((Event.apply _).tupled, Event.unapply)
  }

  lazy val events = TableQuery[Events]

  def formApply(name: String, geographicPoint: Option[String], description: Option[String], startTime: DateTime,
                endTime: Option[DateTime], ageRestriction: Int, tariffRange: Option[String], ticketSellers: Option[String],
                imagePath: Option[String], tariffs: List[Tariff], addresses: List[Address]): Event =
    new Event(None, None, true, true, name, geographicPoint, description, startTime, endTime, ageRestriction,
      tariffRange, ticketSellers, imagePath)//, List.empty, List.empty, tariffs, addresses)

  def formUnapply(event: Event) = {
    Some((event.name, event.geographicPoint, event.description, event.startTime, event.endTime, event.ageRestriction,
      event.tariffRange, event.ticketSellers, event.imagePath, event.tariffs, event.addresses))
  }

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


  /*
    def findOrganizers(): Future[Option[OrganizerWithAddress]] = {
    val chrisQuery = organizers.filter(_.id === 2L)

    val tupledJoin = organizers joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result).map(_.map(OrganizerWithAddress.tupled).headOption)

//    db.run(chrisQuery.result)
  }
   */

  def find(id: Long): Future[Seq[Event]] = {
    val query = events.filter(_.id === id)
    db.run(query.result)//.map(_.map(Event.tupled).headOption)
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Future[Seq[Event]] = {
    val now = DateTime.now()

    val query = events
      .filter(event => event.endTime.nonEmpty)
      .drop(numberToReturn)
      .take(offset)
//      .sortBy(_.)
//      .filter(event => event.endTime > now)
    //&& event.startTime > now
      //event.endTime.get.getMillis() > now.getMillis())
    db.run(query.result)
  }
//
//    DB.withConnection { implicit connection =>
//      SQL(
//        s"""SELECT * FROM events
//           |  WHERE endTime IS NOT NULL AND endTime > CURRENT_TIMESTAMP
//           |        OR endTime IS NULL AND startTime > CURRENT_TIMESTAMP - interval '12 hour'
//           |  ORDER BY geographicPoint <-> point '$geographicPoint'
//           |LIMIT $numberToReturn
//           |OFFSET $offset""".stripMargin)
//        .as(EventParser.*)
//        .map(getPropertiesOfEvent)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Event.findNear: " + e.getMessage)
//  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Event]] =
    Address.findGeographicPointOfCity(city) match {
      case None => Future { Seq.empty }
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }

  def findInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Future[Vector[Event]] = {
    val query = sql"""SELECT * FROM events
             WHERE startTime < (CURRENT_TIMESTAMP + interval '$hourInterval hours')
               AND (endTime IS NOT NULL AND endTime > CURRENT_TIMESTAMP
               OR endTime IS NULL AND startTime > CURRENT_TIMESTAMP - interval '12 hour')
             ORDER BY geographicPoint <-> point '$geographicPoint'
             LIMIT $numberToReturn OFFSET $offset""".as[Event]/*(Long, Option[String], Boolean, Boolean, String, Option[String],
      Option[String], DateTime, Option[DateTime], Option[String], Option[String], Option[String])]*/
    db.run(query)
  }

  def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Future[Vector[Event]] = {
    val query =
      sql"""SELECT * FROM events
              WHERE startTime < CURRENT_TIMESTAMP
                AND startTime > CURRENT_TIMESTAMP - interval '$hourInterval hour'
              ORDER BY geographicPoint <-> point '$geographicPoint '
              LIMIT $numberToReturn OFFSET $offset""".as[Event]
    db.run(query)
  }

  def findAllByGenre(genre: String, geographicPoint: utilities.GeographicPoint, offset: Int, numberToReturn: Int): Future[Vector[Event]] = {
    val geographicPointString = geographicPoint.toString()
    val query = sql"""SELECT e.* FROM eventsGenres eG
                         INNER JOIN events e ON e.eventId = eG.eventId
                           AND (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
                             OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
                         INNER JOIN genres g ON g.genreId = eG.genreId
                         WHERE g.name = $genre
                         ORDER BY geographicPoint <-> point '$geographicPointString'
                         LIMIT $numberToReturn
                         OFFSET $offset""".as[Event]
//        .map(getPropertiesOfEvent)

    db.run(query)
  }

  def findAllByPlace(placeId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.* FROM eventsPlaces eP
          | INNER JOIN events event ON event.eventId = eP.eventId
          |   WHERE eP.placeId = {placeId}
          |     AND (endTime IS NOT NULL AND endTime > CURRENT_TIMESTAMP
          |        OR endTime IS NULL AND startTime > CURRENT_TIMESTAMP - interval '12 hour')
          |ORDER BY event.creationDateTime DESC""".stripMargin)
        .on('placeId -> placeId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByPlace: " + e.getMessage)
  }
  
  def findAllPassedByPlace(placeId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.* FROM eventsPlaces eP
          | INNER JOIN events event ON event.eventId = eP.eventId
          |   WHERE eP.placeId = {placeId}
          |     AND startTime < CURRENT_TIMESTAMP
          |ORDER BY event.creationDateTime DESC""".stripMargin)
        .on('placeId -> placeId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByPlace: " + e.getMessage)
  }

  def findAllByOrganizer(organizerId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.* FROM eventsOrganizers eO
          | INNER JOIN events e ON e.eventId = eO.eventId AND
          |  (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
          |    OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
          |   WHERE eO.organizerId = {organizerId}
          |ORDER BY e.creationDateTime DESC""".stripMargin)
        .on('organizerId -> organizerId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByOrganizer: " + e.getMessage)
  }
  
  def findAllPassedByOrganizer(organizerId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.* FROM eventsOrganizers eP
          | INNER JOIN events event ON event.eventId = eP.eventId
          |   WHERE eP.organizerId = {organizerId}
          |     AND startTime < CURRENT_TIMESTAMP
          |ORDER BY event.creationDateTime DESC""".stripMargin)
        .on('organizerId -> organizerId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByOrganizer: " + e.getMessage)
  }

  def findAllByArtist(facebookUrl: String): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.* FROM eventsArtists eA
          | INNER JOIN events e ON e.eventId = eA.eventId AND
          |   (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
          |     OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
          | INNER JOIN artists a ON a.artistId = eA.artistId
          |   WHERE a.facebookUrl = {facebookUrl}
          |ORDER BY e.creationDateTime DESC""".stripMargin)
        .on('facebookUrl -> facebookUrl)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByArtist: " + e.getMessage)
  }

  def findAllPassedByArtist(artistId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.* FROM eventsArtists eP
          | INNER JOIN events event ON event.eventId = eP.eventId
          |   WHERE eP.artistId = {artistId}
          |     AND startTime < CURRENT_TIMESTAMP
          |ORDER BY event.creationDateTime DESC""".stripMargin)
        .on('artistId -> artistId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByArtist: " + e.getMessage)
  }

  def findAllContaining(pattern: String, geographicPoint: String): Seq[Event] = geographicPoint match {
    case geographicPointPattern(_) =>
      try {
        DB.withConnection { implicit connection =>
          SQL(
            s"""SELECT * FROM events
               |  WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' AND
               |  (endTime IS NOT NULL AND endTime > CURRENT_TIMESTAMP
               |    OR endTime IS NULL AND startTime > CURRENT_TIMESTAMP - interval '12 hour')
               |ORDER BY geographicPoint <-> point '$geographicPoint'
               |LIMIT 20""".stripMargin)
            .on('patternLowCase -> pattern.toLowerCase)
            .as(EventParser.*)
            .map(getPropertiesOfEvent)
        }
      } catch {
        case e: Exception => throw new DAOException("Event.findAllContaining: " + e.getMessage)
      }
    case _ => Seq.empty
  }

  def findAllByCityPattern(cityPattern: String): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.* FROM eventsAddresses eA
          | INNER JOIN events e ON e.eventId = eA.eventId AND
          |  (e.endTime IS NOT NULL AND e.endTime > CURRENT_TIMESTAMP
          |    OR e.endTime IS NULL AND e.startTime > CURRENT_TIMESTAMP - interval '12 hour')
          | INNER JOIN addresses a ON a.addressId = eA.addressId
          |   WHERE a.city LIKE '%'||{patternLowCase}||'%'
          |LIMIT 50""".stripMargin)
        .on('patternLowCase -> cityPattern.toLowerCase)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllContaining: " + e.getMessage)
  }

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

  def delete(eventId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM events WHERE eventId = {eventId}")
        .on('eventId -> eventId)
        .executeUpdate()
    }
  }

  def update(event: Event): Unit = try {
    DB.withConnection { implicit connection =>
      SQL(
        """UPDATE events
          | SET name = {name}, description = {description}, startTime = {startTime}, endTime = {endTime}
          | WHERE facebookId = {facebookId}""".stripMargin)
        .on(
          'facebookId -> event.facebookId,
          'name -> event.name,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Event.update: " + e.getMessage)
  }

  def follow(userId: UUID, eventId: Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO eventsFollowed(userId, eventId)
          | VALUES ({userId}, {eventId})""".stripMargin)
        .on(
          'userId -> userId,
          'eventId -> eventId)
        .executeInsert()
    }
  }

  def unfollow(userId: UUID, eventId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM eventsFollowed
          | WHERE userId = {userId} AND eventId = {eventId}""".stripMargin)
        .on('userId -> userId,
            'eventId -> eventId)
        .executeUpdate()
    }
  }

  def getFollowedEvents(userUUID: UUID): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT e.* FROM events e
            |  INNER JOIN eventsFollowed ef ON e.eventId = ef.eventId
            |WHERE ef.userId = {userId}""".stripMargin)
        .on('userId -> userUUID)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
      }
  } catch {
    case e: Exception => throw new DAOException("Event.getFollowedEvents: " + e.getMessage)
  }

  def isFollowed(userUUID: UUID, eventId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM eventsFollowed
           |  WHERE userId = {userId} AND eventId = {eventId})""".stripMargin)
        .on("userId" -> userUUID,
            "eventId" -> eventId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.isEventFollowed: " + e.getMessage)
  }

  def saveFacebookEventByFacebookId(eventFacebookId: String): Future[Option[Long]] = {
    findEventOnFacebookByFacebookId(eventFacebookId) map { save }
  }

  def findEventOnFacebookByFacebookId(eventFacebookId: String): Future[Event] = {
    WS.url("https://graph.facebook.com/v2.2/" + eventFacebookId)
      .withQueryString(
        "fields" -> "cover,description,name,start_time,end_time,owner,venue",
        "access_token" -> utilities.facebookToken)
      .get()
      .flatMap { readFacebookEvent }
  }

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
      val ticketSellers = Tariff.findTicketSellers(normalizedWebsites)
      val eventuallyMaybeArtistsFromDescription = Artist.getFacebookArtistsByWebsites(normalizedWebsites)
      val eventuallyMaybeArtistsFromTitle =
        Artist.getEventuallyArtistsInEventTitle(Artist.splitArtistNamesInTitle(name), normalizedWebsites)

      for {
        organizer <- eventuallyOrganizer
        artistsFromDescription <- eventuallyMaybeArtistsFromDescription
        artistsFromTitle <- eventuallyMaybeArtistsFromTitle
      } yield {

        val nonEmptyArtists = (artistsFromDescription.flatten.toList ++ artistsFromTitle).distinct
        Artist.saveArtistsAndTheirTracks(nonEmptyArtists)

        val eventGenres = nonEmptyArtists.flatMap(_.genres).distinct
        println("facebookId:" + facebookId)
        println("yo:" + eventGenres)

        Event(None, facebookId, isPublic = true, isActive = true, utilities.refactorEventOrPlaceName(name), None,
          utilities.formatDescription(description), utilities.formatDate(startTime).getOrElse(new DateTime()),
          utilities.formatDate(endTime), 16, findPrices(description), ticketSellers, Option(source), List(organizer).flatten,
          nonEmptyArtists, List.empty, List(address), List.empty, eventGenres)
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