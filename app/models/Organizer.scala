package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import services.Utilities
import play.api.db.DB
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities.geographicPointToString

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
  val token = play.Play.application.configuration.getString("facebook.token")

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

  def findAll: List[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM organizers")
        .as(OrganizerParser.*)
        .map(getOrganizerProperties)
    }
  } catch {
    case e: Exception => throw new DAOException("Method organizer.findAll: " + e.getMessage)
  }

  def findAllByEvent(event: Event): List[Organizer] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsOrganizers eA
             INNER JOIN organizers a ON a.organizerId = eA.organizerId where eA.eventId = {eventId}""")
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

  def save(organizer: Organizer): Option[Long] = try {
    DB.withConnection { implicit connection =>
      val addressId = organizer.address match {
        case None =>
        case Some(address) => Address.save(address)
      }
      val placeIdWithSameFacebookId = SQL(
        """SELECT organizerId FROM organizers
          | WHERE facebookId = {facebookId}""".stripMargin)
        .on("facebookId" -> organizer.facebookId)
        .as(scalar[Long].singleOpt)
      SQL(
        """SELECT insertOrganizer({facebookId}, {name}, {description}, {addressId}, {phone}, {publicTransit},
          |{websites}, {imagePath}, {geographicPoint}, {placeId})""".stripMargin)
        .on(
          'facebookId -> organizer.facebookId,
          'name -> organizer.name,
          'description -> organizer.description,
          'addressId -> addressId,
          'phone -> Utilities.phoneNumbersStringToSet(organizer.phone).mkString(","),
          'publicTransit -> organizer.publicTransit,
          'websites -> organizer.websites,
          'imagePath -> organizer.imagePath,
          'geographicPoint -> organizer.geographicPoint,
          'placeId -> placeIdWithSameFacebookId)
        .as(scalar[Option[Long]].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.save: " + e.getMessage)
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
      case Some(i) => saveEventOrganizerRelation(eventId, i)
      case None => false
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

  def followOrganizer(userId: String, organizerId : Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO organizersFollowed(userId, organizerId)
          | VALUES ({userId}, {organizerId})""".stripMargin)
        .on(
          'userId -> userId,
          'organizerId -> organizerId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Organizer.followOrganizer: " + e.getMessage)
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
}