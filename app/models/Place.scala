package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException
import services.Utilities
import scala.util.Try

case class Place (placeId: Long,
                  name: String,
                  facebookId: Option[String] = None,
                  description: Option[String] = None,
                  webSite: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  images: List[Image] = List(),
                  address : Option[Address] = None)

object Place {
  implicit val placeWrites = Json.writes[Place]

  def formApply(name: String, facebookId: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String]): Place =
    new Place(-1L, name, facebookId, description, webSite, capacity, openingHours)

  def formUnapply(place: Place): Option[(String, Option[String], Option[String], Option[String],
    Option[Int], Option[String])] = Some((place.name, place.facebookId, place.description,
    place.webSite, place.capacity, place.openingHours))

  private val PlaceParser: RowParser[Place] = {
    get[Long]("placeId") ~
      get[String]("name") ~
      get[Option[String]]("facebookId") ~
      get[Option[String]]("description") ~
      get[Option[String]]("webSite") ~
      get[Option[Int]]("capacity") ~
      get[Option[String]]("openingHours") ~
      get[Option[Long]]("addressId") map {
        case placeId ~ name ~ facebookId ~ description ~ webSite ~ capacity ~ openingHours ~ addressId =>
          Place(placeId, name, facebookId, description, webSite, capacity, openingHours, List(), Address.find(addressId))
    }
  }

  def save(place: Place): Option[Long] = {
    Utilities.testIfExist("places", "facebookId", place.facebookId) match {
      case true => None
      case false => try {
        val addressId = place.address.getOrElse(None) match {
          case None => None
          case Some(address: Address) => address.addressId
        }
        DB.withConnection { implicit connection =>
          SQL("""INSERT into places(name, addressId, facebookId, description, webSite, capacity, openingHours)
            values ({name}, {addressId}, {facebookId}, {description}, {webSite}, {capacity}, {openingHours})"""
          ).on(
              'name -> place.name,
              'addressId -> addressId,
              'facebookId -> place.facebookId,
              'description -> place.description,
              'webSite -> place.webSite,
              'capacity -> place.capacity,
              'openingHours -> place.openingHours
            ).executeInsert() match {
            case None => None
            case Some(placeId: Long) =>
              place.images.foreach(image =>
                Image.save(image.copy(placeId = Some(placeId)))
              )
              Some(placeId)
          }
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot save place: " + e.getMessage)
      }
    }
  }

  def findAll: Seq[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places").as(PlaceParser.*).map( p =>
        p.copy(
          images = Image.findAllByPlace(p.placeId).toList
        )
      )
    }
  }

  def findAllIdsAndFacebookIds = {
    Try(
      DB.withConnection { implicit connection =>
        SQL("SELECT placeId, facebookId from places WHERE facebookId IS NOT NULL")
          .as((get[Long]("placeId") ~
            get[String]("facebookId") map {
              case placeId ~ facebookId => (placeId, facebookId) } ).*)
            }
    )
  }

  def findAllContaining(pattern: String): Seq[Place] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM places WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 5")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(PlaceParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Place.findAllContaining: " + e.getMessage)
    }
  }


  def find(placeId: Long): Option[Place] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .as(PlaceParser.singleOpt)
    }.map(p => p.copy(
    images = Image.findAllByPlace(p.placeId).toList) )
  }

  def followPlace(userId : Long, placeId : Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into placesFollowed(userId, placeId) values ({userId}, {placeId})").on(
          'userId -> userId,
          'placeId -> placeId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow place: " + e.getMessage)
    }
  }

  def saveEventPlaceRelation(eventId: Long, placeId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO eventsPlaces (eventId, placeId)
          VALUES ({eventId}, {placeId})""").on(
            'eventId -> eventId,
            'placeId -> placeId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save in eventsPlaces : " + e.getMessage)
    }
  }
}
