package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import securesocial.core.IdentityId
import services.Utilities.{testIfExist, geographicPointToString}

case class Event(eventId: Option[Long],
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 name: String,
                 geographicPoint: Option[String],
                 description: Option[String],
                 startTime: Date,
                 endTime: Option[Date],
                 ageRestriction: Int,
                 tariffRange: Option[String],
                 ticketSellers: Option[String],
                 imagePath: Option[String],
                 organizers: List[Organizer],
                 artists: List[Artist],
                 tariffs: List[Tariff],
                 addresses: List[Address],
                 places: List[Place] = List.empty,
                 genres: Seq[Genre] = Seq.empty)

object Event {
  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r
  def formApply(name: String, geographicPoint: Option[String], description: Option[String], startTime: Date,
                endTime: Option[Date], ageRestriction: Int, tariffRange: Option[String], ticketSellers: Option[String],
                imagePath: Option[String], tariffs: List[Tariff], addresses: List[Address]): Event = {
    new Event(None, None, true, true, name, geographicPoint, description, startTime, endTime, ageRestriction,
      tariffRange, ticketSellers, imagePath, List.empty, List.empty, tariffs, addresses)
  }

  def formUnapply(event: Event) = {
    Some((event.name, event.geographicPoint, event.description, event.startTime, event.endTime, event.ageRestriction,
      event.tariffRange, event.ticketSellers, event.imagePath, event.tariffs, event.addresses))
  }

  private val EventParser: RowParser[Event] = {
    get[Long]("eventId") ~
      get[Option[String]]("facebookId") ~
      get[Boolean]("isPublic") ~
      get[Boolean]("isActive") ~
      get[String]("name") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("description") ~
      get[Date]("startTime") ~
      get[Option[Date]]("endTime") ~
      get[Int]("ageRestriction") ~
      get[Option[String]]("tariffRange") ~
      get[Option[String]]("ticketSellers") ~
      get[Option[String]]("imagePath") map {
      case eventId ~ facebookId ~ isPublic ~ isActive ~ name ~ geographicPoint ~ description ~
        startTime ~ endTime ~ ageRestriction ~ tariffRange ~ ticketSellers ~ imagePath =>
        Event.apply(Some(eventId), facebookId, isPublic, isActive, name, geographicPoint, description, startTime,
          endTime, ageRestriction, tariffRange, ticketSellers, imagePath, List.empty, List.empty, List.empty, List.empty)
    }
  }

  def getPropertiesOfEvent(event: Event): Event = event.eventId match {
    case None => throw new DAOException("Event.getPropertiesOfEvent: event without id has been found")
    case Some(eventId) => event.copy(
      organizers = Organizer.findAllByEvent(event),
      artists = Artist.findAllByEvent(event),
      tariffs = Tariff.findAllByEvent(event),
      places = Place.findAllByEvent(eventId),
      genres = Genre.findAllByEvent(eventId),
      addresses = Address.findAllByEvent(event))
  }

  def find(eventId: Long): Option[Event] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM events WHERE eventId = {eventId}")
        .on('eventId -> eventId)
        .as(EventParser.singleOpt)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.eventId: " + e.getMessage)
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM events
           |  WHERE endTime IS NOT NULL AND endTime > CURRENT_TIMESTAMP
           |        OR endTime IS NULL AND startTime > CURRENT_TIMESTAMP - interval '12 hour'
           |  ORDER BY geographicPoint <-> point '$geographicPoint'
           |LIMIT $numberToReturn
           |OFFSET $offset""".stripMargin)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findNear: " + e.getMessage)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Seq[Event] = try {
    Address.findGeographicPointOfCity(city) match {
      case None => Seq.empty
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findNearCity: " + e.getMessage)
  }

  def findInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Seq[Event]
  = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM events
           |  WHERE startTime < (CURRENT_TIMESTAMP + interval '$hourInterval hours')
           |ORDER BY geographicPoint <-> point '$geographicPoint'
           |LIMIT $numberToReturn OFFSET $offset""".stripMargin)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.find20SinceStartingInInterval: " + e.getMessage)
  }  
  
  def findPassedInHourIntervalNear(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): Seq[Event]
  = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM events
           |  WHERE startTime < CURRENT_TIMESTAMP AND startTime > CURRENT_TIMESTAMP - interval '$hourInterval hour'
           |ORDER BY geographicPoint <-> point '$geographicPoint'
           |LIMIT $numberToReturn OFFSET $offset""".stripMargin)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findPassedInHourIntervalNear: " + e.getMessage)
  }

  def findAllByPlace(placeId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.* FROM eventsPlaces eP
          | INNER JOIN events event ON event.eventId = eP.eventId
          |   WHERE eP.placeId = {placeId} 
          |ORDER BY event.creationDateTime DESC""".stripMargin)
        .on('placeId -> placeId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByPlace: " + e.getMessage)
  }

  def findAllByGenre(genre: String, numberToReturn: Int, offset: Int): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT e.*
           |FROM eventsGenres eG
           |  INNER JOIN events e ON e.eventId = eG.eventId
           |  INNER JOIN genres g ON g.genreId = eG.genreId
           |    WHERE g.name = {genre}
           |ORDER BY e.creationDateTime DESC
           |LIMIT $numberToReturn
           |OFFSET $offset""".stripMargin)
        .on('genre -> genre)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByGenre: " + e.getMessage)
  }

  def findAllByOrganizer(organizerId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.* FROM eventsOrganizers eO
          | INNER JOIN events e ON e.eventId = eO.eventId
          |WHERE eO.organizerId = {organizerId} 
          |ORDER BY e.creationDateTime DESC""".stripMargin)
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
        """SELECT e.*
          |FROM eventsArtists eA
          | INNER JOIN events e ON e.eventId = eA.eventId
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

  def findAllContaining(pattern: String, geographicPoint: String): Seq[Event] = geographicPoint match {
    case geographicPointPattern(_) =>
      try {
        DB.withConnection { implicit connection =>
          SQL(
            s"""SELECT * FROM events
               |  WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%'
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
          | INNER JOIN events e on e.eventId = eA.eventId
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
          event.organizers.foreach { organizer => Organizer.saveWithEventRelation(organizer, eventId) }
          event.tariffs.foreach { tariff => Tariff.save(tariff.copy(eventId = eventId)) }
          event.artists.foreach { artist => Artist.saveWithEventRelation(artist, eventId) }
          event.genres.foreach { genre => Genre.saveWithEventRelation(genre, eventId) }
          Option(eventId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.save: " + e.getMessage)
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

  def followEvent(userId: String, eventId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT id FROM users_login
          | WHERE userId = {userId}""".stripMargin)
        .on('userId -> userId)
        .as(scalar[Option[Long]].single)
      match {
        case None =>
          throw new DAOException("Event.followEvent")
        case Some(id) =>
          SQL(
            """INSERT INTO eventsFollowed(userId, eventId)
              | VALUES ({userId}, {eventId})""".stripMargin)
            .on(
              'userId -> id,
              'eventId -> eventId)
            .executeInsert()
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.follow: " + e.getMessage)
  }

  def getFollowedEvents(userId: IdentityId): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT e.* FROM events e
            |  INNER JOIN eventsFollowed ef ON e.eventId = ef.eventId
            |WHERE ef.userId = {userId}""".stripMargin)
        .on('userId -> userId.userId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
      }
  } catch {
    case e: Exception => throw new DAOException("Event.getFollowedEvents: " + e.getMessage)
  }

  def isEventFollowed(userId: IdentityId, eventId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM eventsFollowed
           |  WHERE userId = {userId} AND eventId = {eventId})""".stripMargin)
        .on("userId" -> userId.userId,
            "eventId" -> eventId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.isEventFollowed: " + e.getMessage)
  }
}