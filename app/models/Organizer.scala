package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.{ThereIsNoOrganizerForThisFacebookIdException, DAOException}
import play.api.libs.json._
import play.api.libs.ws.{WS, Response}
import securesocial.core.IdentityId
import services.Utilities
import play.api.db.DB
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.Try
import play.api.libs.functional.syntax._

case class Organizer (organizerId: Option[Long],
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      addressId: Option[Long] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      websites: Option[String] = None,
                      verified: Boolean = false,
                      imagePath: Option[String] = None,
                      geographicPoint: Option[String] = None,
                      address: Option[Address] = None,
                      linkedPlaceId: Option[Long] = None)

object Organizer {

  def formApply(facebookId: Option[String], name: String, imagePath: Option[String]): Organizer =
    new Organizer(None, facebookId, name, imagePath)
  def formUnapply(organizer: Organizer): Option[(Option[String], String, Option[String])] =
    Some((organizer.facebookId, organizer.name, organizer.imagePath))

  private val OrganizerParser: RowParser[Organizer] = {
    get[Long]("organizerId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") ~
      get[Option[Long]]("addressId") ~
      get[Option[String]]("phone") ~
      get[Option[String]]("publicTransit") ~
      get[Option[String]]("websites") ~
      get[Boolean]("verified") ~
      get[Option[String]]("imagePath") ~
      get[Option[String]]("geographicPoint") ~
      get[Option[Long]]("placeId") map {
      case organizerId ~ facebookId ~ name ~ description ~ addressId ~ phone ~ publicTransit ~ websites ~ verified ~
        imagePath ~ geographicPoint ~ placeId =>
        Organizer(Option(organizerId), facebookId, name, description, addressId, phone, publicTransit, websites,
          verified, imagePath, geographicPoint, None, placeId)
    }
  }

  def getOrganizerProperties(organizer: Organizer): Organizer = organizer.copy(
    address = Address.find(organizer.addressId)
  )

  def findAll(numberToReturn: Int, offset: Int): List[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM organizers
          |LIMIT $numberToReturn
          |OFFSET $offset""".stripMargin)
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM eventsOrganizers eA
          | INNER JOIN organizers a ON a.organizerId = eA.organizerId
          | WHERE eA.eventId = {eventId}""".stripMargin)
        .on('eventId -> event.eventId)
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.findAll: " + e.getMessage)
  }

  def find(organizerId: Long): Option[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM organizers WHERE organizerId = {organizerId}")
        .on('organizerId -> organizerId)
        .as(OrganizerParser.singleOpt)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Method organizer.find: " + e.getMessage)
  }

  def findAllContaining(pattern: String): Seq[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM organizers WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.findAllContaining: " + e.getMessage)
  }
  
  def findIdByFacebookId(facebookId: Option[String])(implicit connection: Connection): Option[Long] = {
    SQL(
      """SELECT organizerId FROM organizers
        | WHERE facebookId = {facebookId}""".stripMargin)
      .on("facebookId" -> facebookId)
      .as(scalar[Long].singleOpt)
  }

  def save(organizer: Organizer): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      val addressId = organizer.address match {
        case None => None
        case Some(address) => Address.save(Option(address))
      }
      val placeIdWithSameFacebookId = Place.findIdByFacebookId(organizer.facebookId)
      val phoneNumbers = Utilities.phoneNumbersSetToOptionString(Utilities.phoneNumbersStringToSet(organizer.phone)) 
      SQL(
        """SELECT insertOrganizer({facebookId}, {name}, {description}, {addressId}, {phone}, {publicTransit},
          |{websites}, {imagePath}, {geographicPoint}, {placeId})""".stripMargin)
        .on(
          'facebookId -> organizer.facebookId,
          'name -> organizer.name,
          'description -> organizer.description,
          'addressId -> addressId,
          'phone -> phoneNumbers,
          'publicTransit -> organizer.publicTransit,
          'websites -> organizer.websites,
          'imagePath -> organizer.imagePath,
          'geographicPoint -> organizer.geographicPoint,
          'placeId -> placeIdWithSameFacebookId)
        .as(scalar[Option[Long]].single)
    }
  }

  def findIdByName(name: String): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("SELECT organizerId FROM organizers WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].singleOpt)
    }
  }

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): Seq[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT * FROM organizers
           |ORDER BY geographicPoint <-> point '$geographicPoint'
                                                                  |LIMIT $numberToReturn
            |OFFSET $offset""".stripMargin)
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.findNear: " + e.getMessage)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Seq[Organizer] = try {
    Address.findGeographicPointOfCity(city) match {
      case None => Seq.empty
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.findNearCity: " + e.getMessage)
  }

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Boolean = {
    save(organizer) match {
      case Success(Some(organizerId)) => saveEventRelation(eventId, organizerId)
      case Success(None) => false
      case Failure(_) => false
    }
  }

  def saveEventRelation(eventId: Long, organizerId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL( """SELECT insertEventOrganizerRelation({eventId}, {organizerId})""")
        .on(
          'eventId -> eventId,
          'organizerId -> organizerId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save in saveEventOrganizerRelation: " + e.getMessage)
  }

  def deleteEventRelation(eventId: Long, organizerId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(s"""DELETE FROM eventsOrganizers WHERE eventId = $eventId AND organizerId = $organizerId""")
        .executeUpdate()
    }
  }

  def delete(organizerId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM organizers WHERE organizerId = {organizerId}")
        .on('organizerId -> organizerId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.delete : " + e.getMessage)
  }

  def followByOrganizerId(userId : String, organizerId : Long): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO organizersFollowed(userId, organizerId)
          |VALUES({userId}, {organizerId})""".stripMargin)
        .on(
          'userId -> userId,
          'organizerId -> organizerId)
        .executeInsert()
    }
  }

  def followByFacebookId(userId : String, facebookId: String): Try[Option[Long]] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT organizerId FROM organizers WHERE facebookId = {facebookId}""")
        .on('facebookId -> facebookId)
        .as(scalar[Long].singleOpt) match {
        case None => throw new ThereIsNoOrganizerForThisFacebookIdException("Organizer.followOrganizerIdByFacebookId")
        case Some(organizerId) => followByOrganizerId(userId, organizerId)
      }
    }
  }

  def unfollowByOrganizerId(userId: String, organizerId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM organizersFollowed
          | WHERE userId = {userId} AND organizerId = {organizerId}""".stripMargin)
        .on('userId -> userId,
          'organizerId -> organizerId)
        .executeUpdate()
    }
  }

  def isFollowed(userId: IdentityId, organizerId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT exists(SELECT 1 FROM organizersFollowed
          |  WHERE userId = {userId} AND organizerId = {organizerId})""".stripMargin)
        .on("userId" -> userId.userId,
          "organizerId" -> organizerId)
        .as(scalar[Boolean].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.isOrganizerFollowed: " + e.getMessage)
  }

  def getFollowedOrganizers(userId: IdentityId): Seq[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("""select a.* from organizers a
            |  INNER JOIN organizersFollowed af ON a.organizerId = af.organizerId
            |WHERE af.userId = {userId}""".stripMargin)
        .on('userId -> userId.userId)
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.getFollowedOrganizers: " + e.getMessage)
  }

  def readOrganizer(organizer: Response, organizerId: String): Option[Organizer] = {
    val readOrganizer = (
      (__ \ "name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "cover" \ "source").readNullable[String] and
        (__ \ "location" \ "street").readNullable[String] and
        (__ \ "location" \ "zip").readNullable[String] and
        (__ \ "location" \ "city").readNullable[String] and
        (__ \ "phone").readNullable[String] and
        (__ \ "public_transit").readNullable[String] and
        (__ \ "website").readNullable[String])
      .apply((name: String, description: Option[String], source: Option[String], street: Option[String],
              zip: Option[String], city: Option[String], phone: Option[String], public_transit: Option[String],
              website: Option[String]) =>
      Organizer(None, Some(organizerId), name, Utilities.formatDescription(description), None, phone, public_transit,
        website, verified = false, imagePath = source, geographicPoint = None, address = Option(Address(None, None,
          city, zip, street)))
      )
    organizer.json.asOpt[Organizer](readOrganizer)
  }

  def getOrganizerInfo(maybeOrganizerId: Option[String]): Future[Option[Organizer]] = maybeOrganizerId match {
    case None => Future { None }
    case Some(organizerId) =>
      WS.url("https://graph.facebook.com/v2.2/" + organizerId)
        .withQueryString(
          "fields" -> "name,description,cover{source},location,phone,public_transit,website",
          "access_token" -> facebookToken)
        .get()
        .map { response => readOrganizer(response, organizerId) }
  }
}