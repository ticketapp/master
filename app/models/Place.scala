package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import controllers.{ThereIsNoPlaceForThisFacebookIdException, DAOException}
import securesocial.core.IdentityId
import services.Utilities.{geographicPointToString, getNormalizedWebsitesInText}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.util.{Success, Failure, Try}
import services.Utilities
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
                  address : Option[Address] = None,
                  linkedOrganizerId: Option[Long] = None)

object Place {
  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String], street: Option[String], zip: Option[String], city: Option[String]): Place = {
    try {
      val address = Option(Address(None, None, city, zip, street))
      new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath, address)
    } catch {
      case e: IllegalArgumentException =>
        new Place(None, name, facebookId, geographicPoint, description, webSite, capacity, openingHours, imagePath, None)
    }
  }

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
      get[Option[String]]("imagePath") ~
      get[Option[Long]]("organizerId") map {
      case placeId ~ name ~ facebookId ~ geographicPoint ~ description ~ webSites ~ capacity ~ openingHours ~
        addressId  ~ imagePath ~ organizerId =>
          Place(Option(placeId), name, facebookId, geographicPoint, description, webSites, capacity, openingHours,
            imagePath, Address.find(addressId), organizerId)
    }
  }

  def delete(placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM places WHERE placeId = {placeId}")
        .on('placeId -> placeId)
        .executeUpdate()
    }
  }

  def save(place: Place): Future[Try[Option[Long]]] = {
    val eventuallyAddressId = saveAddressInFutureWithGeoPoint(place.address)
    eventuallyAddressId map {
      case Success(addressId) =>
        DB.withConnection { implicit connection =>
          Try {
            SQL(
              s"""SELECT insertPlace({name}, {geographicPoint}, {addressId}, {facebookId}, {description},
                 |{webSites}, {capacity}, {openingHours}, {imagePath}, {organizerId})""".stripMargin)
              .on(
                'name -> place.name,
                'geographicPoint -> place.geographicPoint,
                'addressId -> addressId,
                'facebookId -> place.facebookId,
                'description -> place.description,
                'webSites -> Utilities.websiteSetToString(getNormalizedWebsitesInText(place.webSites)),
                'capacity -> place.capacity,
                'openingHours -> place.openingHours,
                'imagePath -> place.imagePath,
                'organizerId -> Organizer.findIdByFacebookId(place.facebookId))
              .as(scalar[Long].singleOpt)
          }
        }
      case Failure(e) =>
        throw e
    }
  }

  def saveAddressInFutureWithGeoPoint(placeAddress: Option[Address]): Future[Try[Option[Long]]] = placeAddress match {
    case Some(address) if address.geographicPoint.isEmpty =>
      Address.getGeographicPoint(address) map { addressWithGeoPoint =>
        Address.save(Option(addressWithGeoPoint)) }
    case Some(addressWithGeoPoint) =>
      Future  { Address.save(Option(addressWithGeoPoint)) }
    case _ =>
      Future { Success(None) }
  }

  def findIdByFacebookId(placeFacebookId: Option[String])(implicit connection: Connection): Option[Long] = {
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

  def findIdByFacebookId(facebookId: String): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT placeId FROM places WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt)
    }
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

  def unfollowByPlaceId(userId: String, placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM placesFollowed
          | WHERE userId = {userId} AND placeId = {placeId}""".stripMargin)
        .on('userId -> userId,
          'placeId -> placeId)
        .executeUpdate()
    }
  }

  def followByFacebookId(userId : String, facebookId: String): Try[Option[Long]] =
    findIdByFacebookId(facebookId) match {
      case Success(Some(placeId)) => followByPlaceId(userId, placeId)
      case Success(None)=> Failure(ThereIsNoPlaceForThisFacebookIdException("Place.followByFacebookId"))
      case failure => failure
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

  def getFollowedPlaces(userId: IdentityId): Seq[Place] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from places a
            |  INNER JOIN placesFollowed af ON a.placeId = af.placeId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userId.userId)
        .as(PlaceParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Place.getFollowedPlaces: " + e.getMessage)
  }

  def saveEventRelation(eventId: Long, placeId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertEventPlaceRelation({eventId}, {placeId})""")
        .on(
          'eventId -> eventId,
          'placeId -> placeId)
        .execute()
    }
  } catch {
    case e: Exception =>
      Logger.error(s"Place.saveEventRelation: error with eventId $eventId and placeId $placeId", e)
      throw new DAOException("Place.saveEventRelation: " + e.getMessage)
  }
  
  def deleteEventRelation(eventId: Long, placeId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(s"""DELETE FROM eventsPlaces WHERE eventId = $eventId AND placeId = $placeId""")
        .executeUpdate()
    }
  }
}
