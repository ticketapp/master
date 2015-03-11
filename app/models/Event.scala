package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import play.libs.Scala
import services.Utilities.{testIfExist, geographicPointToString}

case class Event(eventId: Long,
                 facebookId: Option[String],
                 isPublic: Boolean,
                 isActive: Boolean,
                 creationDateTime: Date,
                 name: String,
                 geographicPoint: Option[String],
                 description: Option[String],
                 startTime: Date,
                 endTime: Option[Date],
                 ageRestriction: Int,
                 images: List[Image],
                 organizers: List[Organizer],
                 artists: List[Artist],
                 tariffs: List[Tariff],
                 addresses: List[Address],
                 places: List[Place] = List.empty,
                 genres: Set[Genre] = Set.empty)

object Event {
  def formApply(name: String, geographicPoint: Option[String], description: Option[String], startTime: Date,
                endTime: Option[Date], ageRestriction: Int, images: List[Image], tariffs: List[Tariff],
                addresses: List[Address]): Event = {
    new Event(-1L, None, true, true, new Date, name, geographicPoint, description, startTime, endTime, ageRestriction,
      images, List(), List(), tariffs, addresses)
  }

  def formUnapply(event: Event): Option[(String, Option[String], Option[String], Date, Option[Date], Int, List[Image],
    List[Tariff], List[Address])] = {
    Some((event.name, event.geographicPoint, event.description, event.startTime, event.endTime, event.ageRestriction,
      event.images, event.tariffs, event.addresses))
  }

  private val EventParser: RowParser[Event] = {
    get[Long]("eventId") ~
      get[Option[String]]("facebookId") ~
      get[Boolean]("isPublic") ~
      get[Boolean]("isActive") ~
      get[Date]("creationDateTime") ~
      get[String]("name") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("description") ~
      get[Date]("startTime") ~
      get[Option[Date]]("endTime") ~
      get[Int]("ageRestriction") map {
      case eventId ~ facebookId ~ isPublic ~ isActive ~ creationDateTime ~ name ~ geographicPoint ~ description ~
        startTime ~ endTime ~ ageRestriction =>
        Event.apply(eventId, facebookId, isPublic, isActive, creationDateTime, name, geographicPoint, description,
          startTime, endTime, ageRestriction, List(), List(), List(), List(), List())
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
          places = Place.findAllByEvent(e),
          addresses = Address.findAllByEvent(e))
        )
    }
  }

  def find20Since(start: Int): Seq[Event] = {
    /*
    change limit by variable?
     */
    try {
      DB.withConnection { implicit connection =>
        SQL( s""" SELECT events.eventId, events.facebookId, events.isPublic, events.isActive, events.creationDateTime,
         events.name, events.geographicPoint, events.description, events.startTime,
            events.endTime, events.ageRestriction
        FROM events
        ORDER BY events.creationDateTime DESC
        LIMIT 20
        OFFSET $start""")
          .as(EventParser.*)
          .map(e => e.copy(
          images = Image.findAllByEvent(e),
          organizers = Organizer.findAllByEvent(e),
          artists = Artist.findAllByEvent(e),
          tariffs = Tariff.findAllByEvent(e),
          places = Place.findAllByEvent(e),
          addresses = Address.findAllByEvent(e))
          )
      }
    }
  }

  def findAllByPlace(placeId: Long) = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT s.eventId, s.facebookId, s.isPublic, s.isActive, s.creationDateTime,
              s.name, s.geographicPoint, s.description, s.startTime,
              s.endTime, s.ageRestriction
              FROM eventsPlaces eP
              INNER JOIN events s ON s.eventId = eP.eventId
              WHERE eP.placeId = {placeId}
              ORDER BY s.creationDateTime DESC
              LIMIT 20""")
        .on('placeId -> placeId)
        .as(EventParser.*)
        .map(event => event.copy(
          images = Image.findAllByEvent(event),
          organizers = Organizer.findAllByEvent(event),
          artists = Artist.findAllByEvent(event),
          tariffs = Tariff.findAllByEvent(event),
          places = Place.findAllByEvent(event),
          addresses = Address.findAllByEvent(event))
        )
    }
  }

  def findAllByGenre(genreId: Long) = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """ SELECT e.eventId, e.facebookId, e.isPublic, e.isActive, e.creationDateTime,
              e.name, e.geographicPoint, e.description, e.startTime,
              e.endTime, e.ageRestriction
          FROM eventsGenres eG
          INNER JOIN events e ON e.eventId = eG.eventId
          WHERE eG.genreId = {genreId}
          ORDER BY e.creationDateTime DESC
          LIMIT 20""")
          .on('genreId -> genreId)
          .as(EventParser.*)
          .map { event => event.copy(
            images = Image.findAllByEvent(event),
            organizers = Organizer.findAllByEvent(event),
            artists = Artist.findAllByEvent(event),
            tariffs = Tariff.findAllByEvent(event),
            addresses = Address.findAllByEvent(event))
          }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot get events by genre: " + e.getMessage)
    }
  }

  def findAllByOrganizer(organizerId: Long) = {
    DB.withConnection { implicit connection =>
      SQL(""" SELECT s.eventId, s.facebookId, s.isPublic, s.isActive, s.creationDateTime,
            s.name, s.geographicPoint, s.description, s.startTime,
            s.endTime, s.ageRestriction
        FROM eventsOrganizers eO
        INNER JOIN events s ON s.eventId = eO.eventId
        WHERE eO.organizerId = {organizerId}
        ORDER BY s.creationDateTime DESC
        LIMIT 20""")
        .on('organizerId -> organizerId)
        .as(EventParser.*)
        .map(event => event.copy(
          images = Image.findAllByEvent(event),
          organizers = Organizer.findAllByEvent(event),
          artists = Artist.findAllByEvent(event),
          tariffs = Tariff.findAllByEvent(event),
          places = Place.findAllByEvent(event),
          addresses = Address.findAllByEvent(event))
        )
    }
  }

  def findAllContaining(pattern: String, center: String): Seq[Event] = {
    val patternRegex = """(\(-?\d+\.\d*,-?\d+\.\d*\))""".r
    center match {
      case patternRegex(_) =>
      try {
        DB.withConnection { implicit connection =>
          SQL(s"""SELECT *
          FROM events WHERE LOWER(name)
          LIKE '%'||{patternLowCase}||'%'
          ORDER BY geographicPoint <-> point '$center'
          LIMIT 20""")
            .on('patternLowCase -> pattern.toLowerCase)
            .as(EventParser.*)
            .map(event => event.copy(
              images = Image.findAllByEvent(event),
              organizers = Organizer.findAllByEvent(event),
              artists = Artist.findAllByEvent(event),
              tariffs = Tariff.findAllByEvent(event),
              places = Place.findAllByEvent(event),
              addresses = Address.findAllByEvent(event))
            )
        }
      } catch {
        case e: Exception => throw new DAOException("Problem with the method Event.findAllContaining: " + e.getMessage)
      }
    case _ => Seq.empty
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
          .map(event => event.copy(
            images = Image.findAllByEvent(event),
            organizers = Organizer.findAllByEvent(event),
            artists = Artist.findAllByEvent(event),
            tariffs = Tariff.findAllByEvent(event),
            places = Place.findAllByEvent(event),
            addresses = Address.findAllByEvent(event))
          )
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Event.findAllContaining: " + e.getMessage)
    }
  }

  def save(event: Event): Option[Long] = {
    testIfExist("events", "facebookId", event.facebookId) match {
      case true => None
      case false =>
        //+ négatif
        val pattern = """(\(\d+\.?\d*,\d+\.?\d*\))""".r
        val geographicPoint = event.geographicPoint.getOrElse("(0,0)")
        println(geographicPoint)
        geographicPoint match {
          case pattern(_) =>
            try {
              DB.withConnection { implicit connection =>
                SQL(s"""INSERT INTO
                    events(facebookId, isPublic, isActive, creationDateTime, name, geographicPoint, description,
                    startTime, endTime, ageRestriction) values ({facebookId}, {isPublic}, {isActive},
                    {creationDateTime}, {name}, point '$geographicPoint', {description}, {startTime}, {endTime},
                    {ageRestriction}) """)
                  .on(
                    'facebookId -> event.facebookId,
                    'isPublic -> event.isPublic,
                    'isActive -> event.isActive,
                    'creationDateTime -> event.creationDateTime,
                    'name -> event.name,
                    'geographicPoint -> event.geographicPoint,
                    'description -> event.description,
                    'startTime -> event.startTime,
                    'endTime -> event.endTime,
                    'ageRestriction -> event.ageRestriction
                  ).executeInsert() match {
                  case None => None
                  case Some(eventId: Long) =>
                    event.organizers.foreach { organizer =>
                      Organizer.saveWithEventRelation (organizer, eventId)
                    }
                    event.tariffs.foreach { tariff =>
                      Tariff.save(tariff.copy(eventId = eventId))
                    }
                    event.images.foreach { image =>
                      Image.save(image.copy(eventId = Some(eventId)))
                    }
                    event.artists.foreach { artist =>
                      println(artist)
                      Artist.saveWithEventRelation(artist, eventId)
                    }
                    Option(eventId)
                }
              }
            } catch {
              case e: Exception => throw new DAOException("Cannot save event: (Event.save method) " + e.getMessage)
            }
          case _ =>
            println("geographicPoint not conform")
            None
        }
    }
  }

  def update(event: Event): Unit = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """UPDATE events
          SET name={name}, description={description}, startTime={startTime}, endTime={endTime}
          WHERE facebookId={facebookId}""")
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
      case e: Exception => throw new DAOException("Cannot update event: " + e.getMessage)
    }
  }


  //def upsert(event: Event) = TODO




  def followEvent(userId: String, eventId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("""select id from users_login where userId = {userId}"""
        ).on(
          'userId -> userId
        ).as(scalar[Option[Long]].single) match {
          case None => throw new DAOException("Cannot follow event: didn't find id with this userId")
          case Some(id) => SQL("""INSERT INTO eventsFollowed(userId, eventId)
          VALUES ({userId}, {eventId})""").on(
              'userId -> id,
              'eventId -> eventId
            ).executeInsert()
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow event: " + e.getMessage)
    }
  }

  def findAllInCircle(center: String): List[Event] = {
    val pattern = """(\(\d+\.\d*,\d+\.\d*\))""".r
    center match {
      case pattern(_) =>
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

