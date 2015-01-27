package models

import controllers.DAOException
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json
import play.api.libs.json._
import java.util.Date

import scala.collection.mutable.ListBuffer

case class Event(eventId: Long,
                 isPublic: Boolean,
                 isActive: Boolean,
                 creationDateTime: Date,
                 name: String,
                 startSellingTime: Date,
                 endSellingTime: Date,
                 description: String,
                 startTime: Date,
                 endTime: Date,
                 ageRestriction: Int,
                 images: List[Image],
                 users: List[User],
                 places: List[Place],
                 artists: List[Artist],
                 tariffs: List[Tariff])

object Event {
  def formApply(name: String, startSellingTime: Date, endSellingTime: Date, description: String, startTime: Date,
                endTime: Date, ageRestriction: Int, tariffs: Seq[Tariff]
                /*denominations: List[String], nbTicketToSells: List[Int], prices: List[BigDecimal],
                startTimes: List[Date], endTimes: List[Date]*/): Event = {

    println(tariffs)
    /*(placeId: Long,
      name: String,
      eventId: Option[Long],
      addressID: Option[Long])

    var newPlaces = new ListBuffer[Place]()
    (paths, alts).zipped.foreach((path, alt) => newPlaces += new Place(-1L, name, alt))

    var newTariffs = new ListBuffer[Tariff]()
    ((denominations, nbTicketToSells, prices).zipped, startTimes, endTimes).zipped.foreach { 
        case ((denomination, nbTicketToSell, price), startTime, endTime) =>
          newTariffs += new Tariff(-1L, denomination, nbTicketToSell, 0, price, startTime, endTime, -1L)
      }
*/
    val images = List()
    new Event(-1L, true, true, new Date, name, startSellingTime, endSellingTime, description, startTime,
              endTime, ageRestriction, images, List(), List(), List(), tariffs.toList)//newTariffs.toList)
  }

  def formUnapply(event: Event): Option[(String, Date, Date, String, Date, Date, Int, Seq[Tariff])] = {
    Some((event.name, event.startSellingTime, event.endSellingTime, event.description, event.startTime,
          event.endTime, event.ageRestriction, event.tariffs.toSeq))
  }

  private val EventParser: RowParser[Event] = {
    get[Long]("eventId") ~
    get[Boolean]("isPublic") ~
    get[Boolean]("isActive") ~
    get[Date]("creationDateTime") ~
    get[String]("name") ~
    get[Date]("startSellingTime") ~
    get[Date]("endSellingTime") ~
    get[String]("description") ~
    get[Date]("startTime") ~
    get[Date]("endTime") ~
    get[Int]("ageRestriction")  map {
      case eventId ~ isPublic ~ isActive ~ creationDateTime ~ name ~ startSellingTime
        ~ endSellingTime ~ description ~ startTime
        ~ endTime ~ ageRestriction  =>
        Event.apply(eventId, isPublic, isActive, creationDateTime, name, startSellingTime,
          endSellingTime, description, startTime, endTime,
          ageRestriction, List(), List(), List(), List(), List())
    }
  }


  def find(eventId: Long): Option[Event] = {
    DB.withConnection { implicit connection =>
      val eventResultSet = SQL("SELECT * from events WHERE eventId = {eventId}")
        .on('eventId -> eventId)
        .as(EventParser.singleOpt)
      eventResultSet.map(e => e.copy(
        images = Image.findAllByEvent(e).toList,
        users = User.findAllByEvent(e).toList,
        places = Place.findAllByEvent(e).toList,
        artists = Artist.findAllByEvent(e).toList,
        tariffs = Tariff.findAllByEvent(e).toList))
    }
  }

  def findAll() = {
    DB.withConnection { implicit connection =>
      val eventsResultSet = SQL(
        """ SELECT events.eventId, events.isPublic, events.isActive, events.creationDateTime, events.name,
        events.startSellingTime, events.endSellingTime, events.description, events.startTime, events.endTime,
        events.ageRestriction
        FROM events
        ORDER BY events.creationDateTime DESC""").as(EventParser *)
      eventsResultSet.map(e => e.copy(
        images = Image.findAllByEvent(e).toList,
        users = User.findAllByEvent(e).toList,
        places = Place.findAllByEvent(e).toList,
        artists = Artist.findAllByEvent(e).toList))
    }
  }

  def findAllStartingWith(pattern: String): Seq[Event] = {
    /*

    Security with the string? Need to escape it?


     */
    var patternLowCase = pattern.toLowerCase()
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM events WHERE LOWER(name) LIKE {patternLowCase} || '%' LIMIT 10")
          .on('patternLowCase -> patternLowCase)
          .as(EventParser *)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Event.findAllStartingWith: " + e.getMessage)
    }
  }

  def saveEvent(event: Event): Long = {
    var eventIdToReturn: Long = 0
    try {
      eventIdToReturn = DB.withConnection { implicit connection =>
        SQL("""insert into events(isPublic, isActive, creationDateTime, name, startSellingTime, endSellingTime, description,
          startTime, endTime, ageRestriction) values ({isPublic}, {isActive}, {creationDateTime}, {name},
          {startSellingTime}, {endSellingTime}, {description}, {startTime}, {endTime}, {ageRestriction})""").on(
          'isPublic -> event.isPublic,
          'isActive -> event.isActive,
          'creationDateTime -> event.creationDateTime,
          'name -> event.name,
          'startSellingTime -> event.startSellingTime,
          'endSellingTime -> event.endSellingTime,
          'description -> event.description,
          'startTime -> event.startTime,
          'endTime -> event.endTime,
          'ageRestriction -> event.ageRestriction
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save event: " + e.getMessage)
    }

    //save tariffs
    (event.tariffs).foreach(tariff =>
      try {
        DB.withConnection { implicit connection =>
          SQL("""INSERT INTO tariffs (denomination, nbTicketToSell, price, startTime, endTime, eventId)
            VALUES ({denomination}, {nbTicketToSell}, {price}, {startTime}, {endTime}, {eventId})""").on(
              'denomination -> tariff.denomination,
              'nbTicketToSell -> tariff.nbTicketToSell,
              'price -> tariff.price,
              'startTime -> tariff.startTime,
              'endTime -> tariff.endTime,
              'eventId -> eventIdToReturn
            ).executeInsert().get
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot save tariff: " + e.getMessage)
      }
    )
    eventIdToReturn
  }

  def followEvent(userId : Long, eventId : Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into eventsFollowed(userId, eventId) values ({userId}, {eventId})").on(
          'userId -> userId,
          'eventId -> eventId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow event: " + e.getMessage)
    }
  }
}
