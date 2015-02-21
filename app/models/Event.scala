package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import services.Utilities

case class Event(eventId: Long,
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 creationDateTime: Date,
                 name: String,
                 description: String,
                 startTime: Date,
                 endTime: Option[Date],
                 ageRestriction: Int,
                 images: List[Image],
                 organizers: List[Organizer],
                 artists: List[Artist],
                 tariffs: List[Tariff],
                 addresses: List[Address])


object Event {
  def formApply(name: String, description: String, startTime: Date, endTime: Option[Date], ageRestriction: Int,
                images: List[Image], tariffs: List[Tariff], addresses: List[Address]): Event = {
    new Event(-1L, None, true, true, new Date, name, description, startTime, endTime, ageRestriction, images, List(),
      List(), tariffs, addresses)
  }

  def formUnapply(event: Event): Option[(String, String, Date, Option[Date], Int, List[Image], List[Tariff],
    List[Address])] = {
    Some((event.name, event.description, event.startTime, event.endTime, event.ageRestriction, event.images,
      event.tariffs, event.addresses))
  }

  private val EventParser: RowParser[Event] = {
    get[Long]("eventId") ~
      get[Option[String]]("facebookId") ~
      get[Boolean]("isPublic") ~
      get[Boolean]("isActive") ~
      get[Date]("creationDateTime") ~
      get[String]("name") ~
      get[String]("description") ~
      get[Date]("startTime") ~
      get[Option[Date]]("endTime") ~
      get[Int]("ageRestriction") map {
      case eventId ~ facebookId ~ isPublic ~ isActive ~ creationDateTime ~ name ~ description ~ startTime ~ endTime
        ~ ageRestriction =>
        Event.apply(eventId, facebookId, isPublic, isActive, creationDateTime, name, description, startTime, endTime,
          ageRestriction, List(), List(), List(), List(), List())
    }
  }

  def find(eventId: Long): Option[Event] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from events WHERE eventId = {eventId}")
        .on('eventId -> eventId)
        .as(EventParser.singleOpt)
        .map(e => e.copy(
          images = Image.findAllByEvent(e),
          organizers = Organizer.findAllByEvent(e),
          artists = Artist.findAllByEvent(e),
          tariffs = Tariff.findAllByEvent(e),
          addresses = Address.findAllByEvent(e))
        )
    }
  }

  def findAll = {
    /*
    change limit by variable
     */
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT events.eventId, events.facebookId, events.isPublic, events.isActive, events.creationDateTime,
            events.name, events.startSellingTime, events.endSellingTime, events.description, events.startTime,
            events.endTime, events.ageRestriction
        FROM events
        ORDER BY events.creationDateTime DESC
        LIMIT 20"""
      ).as(EventParser.*)
        .map(e => e.copy(
          images = Image.findAllByEvent(e),
          organizers = Organizer.findAllByEvent(e),
          artists = Artist.findAllByEvent(e),
          tariffs = Tariff.findAllByEvent(e),
          addresses = Address.findAllByEvent(e))
        )
    }
  }

  def findAllByPlace(placeId: Long) = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT s.eventId, s.facebookId, s.isPublic, s.isActive, s.creationDateTime,
            s.name, s.startSellingTime, s.endSellingTime, s.description, s.startTime,
            s.endTime, s.ageRestriction
        FROM eventsPlaces eP
        INNER JOIN events s ON s.eventId = eP.eventId
        WHERE eP.placeId = {placeId}
        ORDER BY s.creationDateTime DESC
        LIMIT 20"""
      ).on('placeId -> placeId)
      .as(EventParser.*)
      .map(e => e.copy(
        images = Image.findAllByEvent(e),
        organizers = Organizer.findAllByEvent(e),
        artists = Artist.findAllByEvent(e),
        tariffs = Tariff.findAllByEvent(e),
        addresses = Address.findAllByEvent(e))
        )
    }
  }

  def findAllByOrganizer(organizerId: Long) = {
    DB.withConnection { implicit connection =>
      SQL(
        """ SELECT s.eventId, s.facebookId, s.isPublic, s.isActive, s.creationDateTime,
            s.name, s.startSellingTime, s.endSellingTime, s.description, s.startTime,
            s.endTime, s.ageRestriction
        FROM eventsOrganizers eO
        INNER JOIN events s ON s.eventId = eO.eventId
        WHERE eO.organizerId = {organizerId}
        ORDER BY s.creationDateTime DESC
        LIMIT 20""")
        .on('organizerId -> organizerId)
        .as(EventParser.*)
        .map(e => e.copy(
          images = Image.findAllByEvent(e),
          organizers = Organizer.findAllByEvent(e),
          artists = Artist.findAllByEvent(e),
          tariffs = Tariff.findAllByEvent(e),
          addresses = Address.findAllByEvent(e))
        )
    }
  }

  def findAllContaining(pattern: String): Seq[Event] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM events WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(EventParser.*)
          .map(e => e.copy(
            images = Image.findAllByEvent(e),
            organizers = Organizer.findAllByEvent(e),
            artists = Artist.findAllByEvent(e),
            tariffs = Tariff.findAllByEvent(e),
            addresses = Address.findAllByEvent(e))
          )
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Event.findAllContaining: " + e.getMessage)
    }
  }


  def findAllByCityPattern(cityPattern: String): Seq[Event] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("""SELECT *
          FROM events e
          JOIN eventsAddresses eA on e.eventId = eA.eventId
          JOIN addresses a ON a.addressId = eA.eventId
          WHERE a.isEvent = TRUE AND LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 50""")
          .on('patternLowCase -> cityPattern.toLowerCase)
          .as(EventParser.*)
          .map(e => e.copy(
            images = Image.findAllByEvent(e),
            organizers = Organizer.findAllByEvent(e),
            artists = Artist.findAllByEvent(e),
            tariffs = Tariff.findAllByEvent(e),
            addresses = Address.findAllByEvent(e))
          )
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Event.findAllContaining: " + e.getMessage)
    }
  }

  def save(event: Event): Option[Long] = {
    Utilities.testIfExist("events", "facebookId", event.facebookId) match {
      case true => None
      case false => try {
        DB.withConnection {
          implicit connection =>
            SQL( """INSERT INTO
                events(facebookId, isPublic, isActive, creationDateTime, name, description, startTime, endTime,
                ageRestriction) values ({facebookId}, {isPublic}, {isActive}, {creationDateTime}, {name},
                {description}, {startTime}, {endTime}, {ageRestriction}) """)
              .on(
                'facebookId -> event.facebookId,
                'isPublic -> event.isPublic,
                'isActive -> event.isActive,
                'creationDateTime -> event.creationDateTime,
                'name -> event.name,
                'description -> event.description,
                'startTime -> event.startTime,
                'endTime -> event.endTime,
                'ageRestriction -> event.ageRestriction
              ).executeInsert() match {
                case None => None
                case Some(eventId: Long) =>
                  event.organizers.foreach(organizer =>
                    Organizer.saveWithEventRelation(organizer, eventId)
                  )
                  event.tariffs.foreach(tariff =>
                    Tariff.save(tariff.copy(eventId = eventId))
                  )
                  event.images.foreach(image =>
                    Image.save(image.copy(eventId = Some(eventId)))
                  )
                  event.artists.foreach(artist =>
                    Artist.saveWithEventRelation(artist, eventId)
                  )
                  Some(eventId)
            }
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot save event: (Event.save method) " + e.getMessage)
      }
    }
  }

  def update(event: Event): Unit = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """UPDATE events
          SET name={name}, description={description}, startTime={startTime}, endTime={endTime}
          WHERE facebookId={facebookId}"""
        ).on(
          'facebookId -> event.facebookId,
          'name -> event.name,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime
          ).executeUpdate() match {
          case 1 =>
          case _ =>
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot update event: " + e.getMessage)
    }
  }


  //def upsert(event: Event) = TODO






  def followEvent(userId: Long, eventId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into eventsFollowed(userId, eventId) values ({userId}, {eventId})").on(
          'userId -> userId,
          'eventId -> eventId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow event: " + e.getMessage)
    }
  }

  def findAllInCircle(center: String): List[Event] = {
    val pattern = """(\(\d+\.\d*,\d+\.\d*\))""".r
    center match {
      case pattern(_) => println("ok")
        try {
          DB.withConnection { implicit connection =>
            SQL(s"""SELECT *
            FROM events e
            JOIN eventsAddresses eA on e.eventId = eA.eventId
            JOIN addresses a ON a.addressId = eA.eventId
            WHERE a.isEvent = TRUE
            ORDER BY a.geographicPoint <-> point '$center' LIMIT 50"""
            ).as(EventParser.*)
              .map(e => e.copy(
              images = Image.findAllByEvent(e),
              organizers = Organizer.findAllByEvent(e),
              artists = Artist.findAllByEvent(e),
              tariffs = Tariff.findAllByEvent(e),
              addresses = Address.findAllByEvent(e))
              )
          }
        } catch {
          case e: Exception => throw new DAOException("Problem with the method Event.findAllInCircle: " + e.getMessage)
        }
      case _ => List()
    }
  }
}

