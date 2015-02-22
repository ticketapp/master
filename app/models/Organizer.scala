package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import controllers.WebServiceException
import services.Utilities
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import java.util.Date
import play.api.libs.ws.Response
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import scala.util.{Failure, Success}

case class Organizer (organizerId: Long,
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      website: Option[String] = None,
                      verified: Boolean = false,
                      images: List[Image] = List())

object Organizer {
  val token = play.Play.application.configuration.getString("facebook.token")

  private val OrganizerParser: RowParser[Organizer] = {
    get[Long]("organizerId") ~
      get[Option[String]]("facebookId") ~
      get[String]("name") ~
      get[Option[String]]("description") ~
      get[Option[String]]("phone") ~
      get[Option[String]]("publicTransit") ~
      get[Option[String]]("website") ~
      get[Boolean]("verified") map {
      case organizerId ~ facebookId ~ name ~ description ~ phone ~ publicTransit ~ website ~ verified =>
        Organizer(organizerId, facebookId, name, description, phone, publicTransit, website, verified)
    }
  }

  def findAll(): List[Organizer] = {
    DB.withConnection { implicit connection =>
      SQL("select * from organizers")
        .as(OrganizerParser.*)
        .map(o => o.copy(
          images = Image.findAllByOrganizer(o.organizerId)
        ))
    }
  }

  def findAllByEvent(event: Event): List[Organizer] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM eventsOrganizers eA
             INNER JOIN organizers a ON a.organizerId = eA.organizerId where eA.eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(OrganizerParser.*)
        .map(o => o.copy(
          images = Image.findAllByOrganizer(o.organizerId)
        ))
    }
  }

  def find(organizerId: Long): Option[Organizer] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from organizers WHERE organizerId = {organizerId}")
        .on('organizerId -> organizerId)
        .as(OrganizerParser.singleOpt)
        .map(o => o.copy(
          images = Image.findAllByOrganizer(o.organizerId)
        ))
    }
  }

  def findAllContaining(pattern: String): Seq[Organizer] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM organizers WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(OrganizerParser.*)
          .map(o => o.copy(
            images = Image.findAllByOrganizer(o.organizerId)
        ))
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Organizer.findAllContaining: "
        + e.getMessage)
    }
  }

  def formApply(facebookId: Option[String], name: String): Organizer = new Organizer(-1L, facebookId, name)
  def formUnapply(organizer: Organizer): Option[(Option[String], String)] = Some((organizer.facebookId, organizer.name))

  def save(organizer: Organizer): Option[Long] = {
    Utilities.testIfExist("organizers", "name", organizer.name) match {
      case true => Some(-1)
      case false => try {
        //println(organizer.name)
        DB.withConnection { implicit connection =>
          SQL("""INSERT INTO organizers(name, facebookId, description, phone, publicTransit, website)
            VALUES ({name}, {facebookId}, {description}, {phone}, {publicTransit}, {website})""")
            .on(
              'name -> organizer.name,
              'facebookId -> organizer.facebookId,
              'description -> organizer.description ,
              'phone -> organizer.phone,
              'publicTransit -> organizer.publicTransit,
              'website -> organizer.website
          ).executeInsert() match {
            case None => None
            case Some(organizerId: Long) =>
              organizer.images.foreach( image =>
                Image.save(image.copy(organizerId = Some(organizerId)))
              )
              Some(organizerId)
          }
        }
      } catch {
        case e: Exception => throw new DAOException("Cannot create organizer: " + e.getMessage + organizer.name)
      }
    }
  }

  def returnOrganizerId(name: String): Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT organizerId from organizers WHERE name = {name}")
        .on('name -> name)
        .as(scalar[Long].single)
    }
  }

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Option[Long] = {
    save(organizer) match {
      case Some(-1) => saveEventOrganizerRelation(eventId, returnOrganizerId(organizer.name))
      case Some(i) => saveEventOrganizerRelation(eventId, i)
      case None => None
    }
  }

  def saveEventOrganizerRelation(eventId: Long, organizerId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO eventsOrganizers (eventId, organizerId)
          VALUES ({eventId}, {organizerId})""").on(
            'eventId -> eventId,
            'organizerId -> organizerId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save in eventsOrganizers : " + e.getMessage)
    }
  }

  def deleteOrganizer(organizerId: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM organizers WHERE organizerId={organizerId}").on(
        'organizerId -> organizerId
      ).executeUpdate()
    }
  }

  def followOrganizer(userId : Long, organizerId : Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into organizersFollowed(userId, organizerId) values ({userId}, {organizerId})").on(
          'userId -> userId,
          'organizerId -> organizerId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow organizer: " + e.getMessage)
    }
  }
}