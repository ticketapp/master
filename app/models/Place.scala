package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import controllers.{ThereIsNoPlaceForThisFacebookIdException, DAOException}
import securesocial.core.IdentityId
import services.Utilities.{geographicPointToString, getNormalizedWebsitesInText}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.util.Try
import services.Utilities.geographicPointPattern

case class Place (placeId: Option[Long],
                  name: String,
                  facebookId: Option[String] = None,
                  geographicPoint: Option[String],
                  description: Option[String] = None,
                  webSites: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  imagePath: Option[String] = None,
                  address : Option[Address] = None)

object Place {
  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String], street: Option[String], zip: Option[String], city: Option[String]): Place =
    new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath,
      Option(Address(None, None, city, zip, street)))

  def formUnapply(place: Place) =
    Some((place.name, place.facebookId, place.geographicPoint, place.description, place.webSites, place.capacity,
      place.openingHours, place.imagePath, place.address.get.city, place.address.get.zip, place.address.get.street))

  private val PlaceParser: RowParser[Place] = {
    get[Long]("placeId") ~
      get[String]("name") ~
      get[Option[String]]("facebookId") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[String]]("description") ~
      get[Option[String]]("webSites") ~
      get[Option[Int]]("capacity") ~
      get[Option[String]]("openingHours") ~
      get[Option[Long]]("addressId") ~
      get[Option[String]]("imagePath") map {
      case placeId ~ name ~ facebookId ~ geographicPoint ~ description ~ webSites ~ capacity ~ openingHours ~
        addressId  ~ imagePath =>
          Place(Option(placeId), name, facebookId, geographicPoint, description, webSites, capacity, openingHours,
            imagePath, Address.find(addressId))
    }
  }

  def delete(placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .executeUpdate()
    }
  }

  def save(place: Place): Future[Option[Long]] = try {
      val eventuallyAddressId = saveAddressInFutureWithGeoPoint(place.address)
      eventuallyAddressId map { addressId =>
        DB.withConnection { implicit connection =>
        val organizerId = findOrganizerIdWithSameFacebookId(place.facebookId)
        SQL(
          s"""SELECT insertPlace({name}, {geographicPoint}, {addressId}, {facebookId}, {description},
             |{webSites}, {capacity}, {openingHours}, {imagePath}, {organizerId})""".stripMargin)
          .on(
            'name -> place.name,
            'geographicPoint -> place.geographicPoint,
            'addressId -> addressId,
            'facebookId -> place.facebookId,
            'description -> place.description,
            'webSites -> getNormalizedWebsitesInText(place.webSites).mkString(","),
            'capacity -> place.capacity,
            'openingHours -> place.openingHours,
            'imagePath -> place.imagePath,
            'organizerId -> organizerId)
          .as(scalar[Option[Long]].single)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Place.save: " + e.getMessage)
  }

  def saveAddressInFutureWithGeoPoint(placeAddress: Option[Address]): Future[Option[Long]] = {
    placeAddress match {
      case None =>
        Future {
          None
        }
      case Some(address) =>
        Address.getGeographicPoint(address) map { addressWithGeoPoint => Address.save(Option(addressWithGeoPoint)) }
    }
  }

  def findOrganizerIdWithSameFacebookId(placeFacebookId: Option[String])(implicit connection: Connection): Option[Long] = {
    SQL(
      """SELECT placeId FROM places
        | WHERE facebookId = {facebookId}""".stripMargin)
      .on("facebookId" -> placeFacebookId)
      .as(scalar[Long].singleOpt)
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM places
           |  ORDER BY geographicPoint <-> point '$geographicPoint'
           |LIMIT $numberToReturn
           |OFFSET $offset""".stripMargin)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findNear: " + e.getMessage)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Seq[Place] = try {
    Address.findGeographicPointOfCity(city) match {
      case None => Seq.empty
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findNearCity: " + e.getMessage)
  }
  
  def findAll: Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places")
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAll: " + e.getMessage)
  }

  def findAllByEvent(eventId: Long): List[Place] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsPlaces eA
          |INNER JOIN places a ON a.placeId = eA.placeId
          |WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> eventId)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllByEvent: " + e.getMessage)
  }

  def findAllAsTupleIdFacebookIdAndGeographicPoint: Seq[(Long, String, Option[String])] = try {
    val placeIdFacebookIdParser = {
      get[Long]("placeId") ~
        get[String]("facebookId") ~
        get[Option[String]]("geographicPoint") map {
        case placeId ~ facebookId ~ geographicPoint => (placeId, facebookId, geographicPoint)
      }
    }
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT placeId, facebookId, geographicPoint 
          |FROM places 
          |WHERE facebookId IS NOT NULL""".stripMargin)
        .as(placeIdFacebookIdParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllIdsAndFacebookIds: " + e.getMessage)
  }

  def findAllContaining(pattern: String): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 5")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.findAllContaining: " + e.getMessage)
  }

  def find(placeId: Long): Option[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .as(PlaceParser.singleOpt)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.find: " + e.getMessage)
  }

  def followByPlaceId(userId : String, placeId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO placesFollowed(userId, placeId) VALUES({userId}, {placeId})""")
        .on(
          'userId -> userId,
          'placeId -> placeId)
        .executeInsert()
    }
  }

  def unfollowByPlaceId(userId: String, placeId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM placesFollowed
          | WHERE userId = {userId} AND placeId = {placeId}""".stripMargin)
        .on('userId -> userId,
          'placeId -> placeId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Place.unFollow: " + e.getMessage)
  }

  def followByFacebookId(userId : String, facebookId: String): Try[Option[Long]] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT placeId FROM places WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt) match {
        case None => throw new ThereIsNoPlaceForThisFacebookIdException("Place.followPlaceIdByFacebookId")
        case Some(placeId) => followByPlaceId(userId, placeId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Place.followPlaceIdByFacebookId: " + e.getMessage)
  }

  def isFollowed(userId: IdentityId, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM placesFollowed
          |  WHERE userId = {userId} AND placeId = {placeId})""".stripMargin)
        .on("userId" -> userId.userId,
          "placeId" -> placeId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.isPlaceFollowed: " + e.getMessage)
  }

  def saveEventPlaceRelation(eventId: Long, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT insertEventPlaceRelation({eventId}, {placeId})""")
        .on(
          'eventId -> eventId,
          'placeId -> placeId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Place.saveEventPlaceRelation: " + e.getMessage)
  }
}
