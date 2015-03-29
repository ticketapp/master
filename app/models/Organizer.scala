package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import services.Utilities
import play.api.db.DB
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

case class Organizer (organizerId: Long,
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      addressId: Option[Long] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      websites: Option[String] = None,
                      verified: Boolean = false,
                      images: List[Image] = List.empty,
                      address: Option[Address] = None)

object Organizer {
  val token = play.Play.application.configuration.getString("facebook.token")

  def formApply(facebookId: Option[String], name: String): Organizer = new Organizer(-1L, facebookId, name)
  def formUnapply(organizer: Organizer): Option[(Option[String], String)] = Some((organizer.facebookId, organizer.name))

  private val OrganizerParser: RowParser[Organizer] = {
    get[Long]("organizerId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") ~
      get[Option[Long]]("addressId") ~
      get[Option[String]]("phone") ~
      get[Option[String]]("publicTransit") ~
      get[Option[String]]("websites") ~
      get[Boolean]("verified") map {
      case organizerId ~ facebookId ~ name ~ description ~ addressId ~ phone ~ publicTransit ~ websites ~ verified =>
        Organizer(organizerId, facebookId, name, description, addressId, phone, publicTransit, websites, verified)
    }
  }

  def getOrganizerProperties(organizer: Organizer): Organizer = organizer.copy(
    images = Image.findAllByOrganizer(organizer.organizerId),
    address = Address.find(organizer.addressId))


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
    case e: Exception => throw new DAOException("Method organizer.findAll: " + e.getMessage)
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
    case e: Exception => throw new DAOException("Problem with the method Organizer.findAllContaining: "
      + e.getMessage)
  }

  def save(organizer: Organizer): Option[Long] = try {
    DB.withConnection { implicit connection =>
      println((organizer.facebookId, organizer.name, organizer.description, organizer.phone, organizer.websites))
      SQL(
        """SELECT insertOrganizer({facebookId}, {name}, {description}, {phone}, {websites})""")
        .on(
          'facebookId -> organizer.facebookId,
          'name -> organizer.name,
          'description -> organizer.description,
          'phone -> organizer.phone,
          'websites -> organizer.websites)
        .as(scalar[Option[Long]].single) match {
        case None => None
        case Some(organizerId: Long) =>
          organizer.images.foreach(image => Image.save(image.copy(organizerId = Some(organizerId))))
          Some(organizerId)
      }
  }
  } catch {
    case e: Exception => throw new DAOException("Organizer.save: " + e.getMessage +
      organizer.name + " " + organizer.websites + " " + organizer.name.length + "/" +
      organizer.websites.getOrElse("").length)
  }


  def returnOrganizerId(name: String): Long = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT organizerId FROM organizers WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot return Organizer Id: " + e.getMessage)
  }

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Option[Long] = {
    save(organizer) match {
      case Some(-1) => saveEventOrganizerRelation(eventId, returnOrganizerId(organizer.name))
      case Some(i) => saveEventOrganizerRelation(eventId, i)
      case None => None
    }
  }

  def saveEventOrganizerRelation(eventId: Long, organizerId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL( """INSERT INTO eventsOrganizers (eventId, organizerId)
        VALUES ({eventId}, {organizerId})""")
        .on(
          'eventId -> eventId,
          'organizerId -> organizerId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save in saveEventOrganizerRelation: " + e.getMessage)
  }

  def deleteOrganizer(organizerId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM organizers WHERE organizerId={organizerId}")
        .on('organizerId -> organizerId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save in delete Organizer: " + e.getMessage)
  }

  def followOrganizer(organizerId : Long) = {//: Option[Long] = {
    /*try {
      DB.withConnection { implicit connection =>
        SQL("insert into organizersFollowed(userId, organizerId) values ({userId}, {organizerId})").on(
          'userId -> userId,
          'organizerId -> organizerId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow organizer: " + e.getMessage)
    }*/
  }
}