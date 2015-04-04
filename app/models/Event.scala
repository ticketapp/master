package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import services.Utilities.geographicPointToString

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
                endTime: Option[Date], ageRestriction: Int, imagePath: Option[String], tariffs: List[Tariff],
                addresses: List[Address]): Event = {
    new Event(None, None, true, true, name, geographicPoint, description, startTime, endTime, ageRestriction,
      imagePath, List.empty, List.empty, tariffs, addresses)
  }

  def formUnapply(event: Event): Option[(String, Option[String], Option[String], Date, Option[Date], Int,
    Option[String], List[Tariff], List[Address])] = {
    Some((event.name, event.geographicPoint, event.description, event.startTime, event.endTime, event.ageRestriction,
      event.imagePath, event.tariffs, event.addresses))
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
      get[Option[String]]("imagePath") map {
      case eventId ~ facebookId ~ isPublic ~ isActive ~ name ~ geographicPoint ~ description ~
        startTime ~ endTime ~ ageRestriction ~ imagePath =>
        Event.apply(Some(eventId), facebookId, isPublic, isActive, name, geographicPoint, description,
          startTime, endTime, ageRestriction, imagePath, List.empty, List.empty, List.empty, List.empty)
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

  def find20Since(start: Int, center: String): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT *
           |FROM events
           |ORDER BY geographicPoint <-> point '$center'
           |LIMIT 20 OFFSET $start""".stripMargin)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.find20Since: " + e.getMessage)
  }

  def find20InHourIntervalWithOffsetNearCenterPoint(start: Int, center: String, hourInterval: Int): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT *
           |FROM events
           |WHERE startTime < (CURRENT_TIMESTAMP + interval '$hourInterval hours')
           |ORDER BY geographicPoint <-> point '$center'
           |LIMIT 20 OFFSET $start""".stripMargin)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.find20SinceStartingInInterval: " + e.getMessage)
  }

  def findAllByPlace(placeId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT event.eventId, event.facebookId, event.isPublic, event.isActive, event.name, event.geographicPoint, 
          |event.description, event.startTime, event.endTime, event.ageRestriction, event.imagePath
          |FROM eventsPlaces eP INNER JOIN events event ON event.eventId = eP.eventId
          |WHERE eP.placeId = {placeId} 
          |ORDER BY event.creationDateTime DESC LIMIT 20""".stripMargin)
        .on('placeId -> placeId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Event.findAllByPlace: " + e.getMessage)
  }

  def findAllByGenre(genreId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.eventId, e.facebookId, e.isPublic, e.isActive, e.name, e.geographicPoint,
          |e.description, e.startTime, e.endTime, e.ageRestriction, e.imagePath
          |FROM eventsGenres eG
          |INNER JOIN events e ON e.eventId = eG.eventId
          |WHERE eG.genreId = {genreId}
          |ORDER BY e.creationDateTime DESC
          |LIMIT 20""".stripMargin)
        .on('genreId -> genreId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot get events by genre: " + e.getMessage)
  }

  def findAllByOrganizer(organizerId: Long): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.eventId, e.facebookId, e.isPublic, e.isActive, e.name, 
          |e.geographicPoint, e.description, e.startTime, e.endTime, e.ageRestriction, e.imagePath
          |FROM eventsOrganizers eO INNER JOIN events e ON e.eventId = eO.eventId
          |WHERE eO.organizerId = {organizerId} 
          |ORDER BY e.creationDateTime DESC LIMIT 20""".stripMargin)
        .on('organizerId -> organizerId)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot get events by organizer: " + e.getMessage)
  }

  def findAllByArtist(facebookUrl: String): Seq[Event] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT e.eventId, e.facebookId, e.isPublic, e.isActive, e.name, e.geographicPoint,
          |e.description, e.startTime, e.endTime, e.ageRestriction, e.imagePath
          |FROM eventsArtists eA INNER JOIN events e ON e.eventId = eA.eventId
          |WHERE eA.artistId = (SELECT artistId FROM artists WHERE facebookUrl = {facebookUrl})
          |ORDER BY e.creationDateTime DESC
          |LIMIT 20""".stripMargin)
        .on('facebookUrl -> facebookUrl)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Even.findAllByArtist: " + e.getMessage)
  }

  def findAllContaining(pattern: String, center: String): Seq[Event] = center match {
    case geographicPointPattern(_) =>
      try {
        DB.withConnection { implicit connection =>
          SQL(
            s"""SELECT * FROM events WHERE LOWER(name)
               |LIKE '%'||{patternLowCase}||'%'
               |ORDER BY geographicPoint <-> point '$center'
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
        """SELECT e.* FROM events e
          |JOIN eventsAddresses eA on e.eventId = eA.eventId
          |JOIN addresses a ON a.addressId = eA.eventId
          |WHERE a.isEvent = TRUE AND LOWER(name)
          |LIKE '%'||{patternLowCase}||'%'
          |LIMIT 50""".stripMargin)
        .on('patternLowCase -> cityPattern.toLowerCase)
        .as(EventParser.*)
        .map(getPropertiesOfEvent)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with the method Event.findAllContaining: " + e.getMessage)
  }

  def save(event: Event): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT insertEvent({facebookId}, {isPublic}, {isActive}, {name}, {geographicPoint},
           |{description}, {startTime}, {endTime}, {imagePath}, {ageRestriction})""".stripMargin)
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
          'ageRestriction -> event.ageRestriction)
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
          |SET name={name}, description={description}, startTime={startTime}, endTime={endTime}
          |WHERE facebookId={facebookId}""".stripMargin)
        .on(
          'facebookId -> event.facebookId,
          'name -> event.name,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime)
        .executeUpdate() match {
        case 1 =>
        case _ =>
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.update: " + e.getMessage)
  }

  def followEvent(userId: String, eventId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT id FROM users_login WHERE userId = {userId}""")
        .on('userId -> userId)
        .as(scalar[Option[Long]].single) match {
        case None => throw new DAOException("Cannot follow event: didn't find id with this userId")
        case Some(id) => SQL(
          """INSERT INTO eventsFollowed(userId, eventId)
            |VALUES ({userId}, {eventId})""".stripMargin)
          .on(
            'userId -> id,
            'eventId -> eventId)
          .executeInsert()
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Event.follow: " + e.getMessage)
  }

  def findAllInCircle(center: String): List[Event] = center match {
    case geographicPointPattern(_) =>
      try {
        DB.withConnection { implicit connection =>
          SQL(
            s"""SELECT * FROM events e
               |JOIN eventsAddresses eA on e.eventId = eA.eventId
               |JOIN addresses a ON a.addressId = eA.eventId
               |WHERE a.isEvent = TRUE
               |ORDER BY a.geographicPoint <-> point '$center'
               |LIMIT 50""".stripMargin)
            .as(EventParser.*)
            .map(getPropertiesOfEvent)
        }
      } catch {
        case e: Exception => throw new DAOException("Event.findAllInCircle: " + e.getMessage)
      }
    case _ => List.empty
  }
}

