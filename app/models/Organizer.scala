package models

import anorm.SqlParser._
import anorm._
import controllers.{ThereIsNoOrganizerForThisFacebookIdException, DAOException}
import securesocial.core.IdentityId
import services.Utilities
import play.api.db.DB
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities.{geographicPointToString, facebookToken}
import scala.util.{Failure, Success}
import scala.util.Try

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
                      address: Option[Address] = None)

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
      get[Option[String]]("geographicPoint") map {
      case organizerId ~ facebookId ~ name ~ description ~ addressId ~ phone ~ publicTransit ~ websites ~ verified ~
        imagePath ~ geographicPoint =>
        Organizer(Option(organizerId), facebookId, name, description, addressId, phone, publicTransit, websites,
          verified, imagePath, geographicPoint)
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

  def save(organizer: Organizer): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      val addressId = organizer.address match {
        case None => None
        case Some(address) => Address.save(Option(address))
      }
      val placeIdWithSameFacebookId = SQL(
        """SELECT organizerId FROM organizers
          | WHERE facebookId = {facebookId}""".stripMargin)
        .on("facebookId" -> organizer.facebookId)
        .as(scalar[Long].singleOpt)
      val phoneNumbers = Utilities.phoneNumbersStringToSet(organizer.phone) match {
        case emptySet: Set[String] if emptySet.isEmpty => None
        case phoneNumbersFound => Option(phoneNumbersFound.mkString(","))
      }
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

  def returnOrganizerId(name: String): Long = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT organizerId FROM organizers WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.returnOrganizerId: " + e.getMessage)
  }

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Boolean = {
    save(organizer) match {
      case Success(Some(organizerId)) => saveEventOrganizerRelation(eventId, organizerId)
      case Success(None) => false
      case Failure(_) => false
    }
  }

  def saveEventOrganizerRelation(eventId: Long, organizerId: Long): Boolean = try {
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

  val organizer = new Organizer(None, Option("facebookId1"), "organizerTest", Option("description"), None, Option("phone"),
    Option("publicTransit"), Option("websites"), verified = true, imagePath = Option("imagePath"),
    geographicPoint = Option("(5.4,5.6)"))

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

  def unfollowByOrganizerId(userId: String, organizerId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM organizersFollowed
          | WHERE userId = {userId} AND organizerId = {organizerId}""".stripMargin)
        .on('userId -> userId,
          'organizerId -> organizerId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.unFollow: " + e.getMessage)
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
}