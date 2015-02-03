package models

import java.util.Date

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException

import scala.util.Try

/**
 * Created by sim on 03/10/14.
 */

case class Place (placeId: Long,
                  name: String,
                  addressId: Option[Long] = None,
                  facebookId: Option[String] = None,
                  description: Option[String] = None,
                  webSite: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None)

object Place {
  implicit val placeWrites = Json.writes[Place]

  def formApply(name: String, addressId : Option[Long], facebookId: Option[String],
                description: Option[String], webSite: Option[String], capacity: Option[Int],
                openingHours: Option[String]): Place =
    new Place(-1L, name, addressId, facebookId, description, webSite, capacity, openingHours)
  def formUnapply(place: Place): Option[(String, Option[Long], Option[String], Option[String], Option[String],
    Option[Int], Option[String])] =
    Some((place.name, place.addressId, place.facebookId, place.description, place.webSite,
      place.capacity, place.openingHours))

  private val PlaceParser: RowParser[Place] = {
    get[Long]("placeId") ~
      get[String]("name") ~
      get[Option[Long]]("addressId") ~
      get[Option[String]]("facebookId") ~
      get[Option[String]]("description") ~
      get[Option[String]]("webSite") ~
      get[Option[Int]]("capacity") ~
      get[Option[String]]("openingHours") map {
        case placeId ~ name ~ addressId ~ facebookId ~ description ~ webSite ~
           capacity ~ openingHours =>
          Place(placeId, name, addressId, facebookId, description, webSite,
            capacity, openingHours)
    }
  }

  def save(place: Place): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("INSERT into places(name, addressId, facebookId, facebookImage, description, webSite, " +
          "capacity, openingHours) values ({name}, {addressId}, {facebookId}, " +
          "{description}, {webSite})").on(
          'name -> place.name,
          'addressId -> place.addressId,
          'facebookId -> place.facebookId,
          'description -> place.description,
          'webSite -> place.webSite,
          'capacity -> place.capacity,
          'openingHours -> place.openingHours
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create place : " + e.getMessage)
    }
  }

  def findAll(): Seq[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places").as(PlaceParser *)
    }
  }
/*
 private val EventParser: RowParser[Event] = {
    get[Long]("eventId") ~
    get[Option[String]]("facebookId") ~
    get[Boolean]("isPublic") ~
    get[Boolean]("isActive") ~
    get[Date]("creationDateTime") ~
    get[String]("name") ~
    get[Option[Date]]("startSellingTime") ~
    get[Option[Date]]("endSellingTime") ~
    get[String]("description") ~
    get[Date]("startTime") ~
    get[Option[Date]]("endTime") ~
    get[Int]("ageRestriction")  map {
      case eventId ~ facebookId ~ isPublic ~ isActive ~ creationDateTime ~ name ~ startSellingTime
        ~ endSellingTime ~ description ~ startTime ~ endTime ~ ageRestriction  =>
        Event.apply(eventId, facebookId, isPublic, isActive, creationDateTime, name, startSellingTime, endSellingTime, description,
          startTime, endTime, ageRestriction, List(), List(), List(), List(), List())
    }
 */

  def findAllIdsAndFacebookIds = {
    Try(
      DB.withConnection { implicit connection =>
        SQL("SELECT placeId, facebookId from places WHERE facebookId IS NOT NULL")
          .as((get[Long]("placeId") ~
            get[String]("facebookId") map {
              case placeId ~ facebookId => (placeId, facebookId) } ) *)
            }
    )
  }

  def findAllByEvent(event: Event): Seq[Place] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsPlaces eP
             INNER JOIN places s ON s.placeId = eP.placeId where eP.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(PlaceParser *)
    }
  }

  def findAllStartingWith(pattern: String): Seq[Place] = {
    /*

    Security with the string? Need to escape it?


     */
    var patternLowCase = pattern.toLowerCase()
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM places WHERE LOWER(name) LIKE {patternLowCase} || '%' LIMIT 5")
          .on('patternLowCase -> patternLowCase)
          .as(PlaceParser *)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Place.findAllStartingWith: " + e.getMessage)
    }
  }

  def find(placeId: Long): Option[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .as(PlaceParser.singleOpt)
    }
  }

  def followPlace(userId : Long, placeId : Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into placesFollowed(userId, placeId) values ({userId}, {placeId})").on(
          'userId -> userId,
          'placeId -> placeId
        ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow place: " + e.getMessage)
    }
  }
}
