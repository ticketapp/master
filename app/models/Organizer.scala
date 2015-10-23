package models

import java.util.UUID
import javax.inject.Inject

import com.vividsolutions.jts.geom.{Geometry, Point}
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}
import slick.model.ForeignKeyAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class Organizer (id: Option[Long],
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      addressId: Option[Long] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      websites: Option[String] = None,
                      verified: Boolean = false,
                      imagePath: Option[String] = None,
                      geographicPoint: Option[Geometry] = None,
                      linkedPlaceId: Option[Long] = None)

case class OrganizerWithAddress(organizer: Organizer, address: Option[Address])

case class OrganizerFollowed(userId: UUID, Organizer: Long)

class OrganizerMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                 val placeMethods: PlaceMethods,
                                 val utilities: Utilities,
                                 val geographicPointMethods: GeographicPointMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def formApply(facebookId: Option[String], name: String, description: Option[String], websites: Option[String],
                imagePath: Option[String]): Organizer =
    Organizer(None, facebookId, name, description = description, websites = websites, imagePath = imagePath)
  def formUnapply(organizer: Organizer) =
    Some((organizer.facebookId, organizer.name, organizer.description, organizer.websites, organizer.imagePath))

//  def getOrganizerProperties(organizer: Organizer): Organizer = organizer.copy(
//    address = Address.find(organizer.addressId)
//  )

  def find(numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] = {
    val tupledJoin = organizers joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result).map(_.map(OrganizerWithAddress.tupled))
  }

  def findAllByEvent(event: Event): Future[Seq[Organizer]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)

    val query = for {
      event <- events if event.id === event.id
      eventOrganizer <- eventsOrganizers
      organizer <- organizers if organizer.id === eventOrganizer.organizerId
    } yield organizer


    //getOrganizerProperties
    db.run(query.result)
  }

  def findById(id: Long): Future[Option[Organizer]] = {
    val query = organizers.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[Organizer]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      organizer <- organizers if organizer.name.toLowerCase like s"%$lowercasePattern%"
    } yield organizer
    //        .map(getOrganizerProperties)
    db.run(query.result)
  }

  def doSave (organizer: Organizer): Future[Organizer] = {
    db.run((for {
      organizerFound <- organizers.filter(_.facebookId === organizer.facebookId).result.headOption
      result <- organizerFound.map(DBIO.successful).getOrElse(organizers returning organizers.map(_.id) += organizer)
    } yield result match {
        case o: Organizer => o
        case id: Long => organizer.copy(id = Option(id))
      }).transactionally)
  }

  def save(organizer: Organizer): Future[Organizer] = {
    val organizerWithFormattedDescription = organizer.copy(description = utilities.formatDescription(organizer.description))
    organizer.facebookId match {
      case None =>
        doSave(organizerWithFormattedDescription)
      case Some(facebookId) =>
        placeMethods.findIdByFacebookId(facebookId) flatMap { maybePlaceId =>
          val organizerWithLinkedPlace = organizerWithFormattedDescription.copy(linkedPlaceId = maybePlaceId)
          doSave(organizerWithLinkedPlace)
        }
    }
  }



//  def save(organizer: Organizer): Future[Long] = {
////    val addressId = organizer.address match {
////      case None => None
////      case Some(address) => Address.save(Option(address))
////    }
//    val placeIdWithSameFacebookId = placeMethods.findIdByFacebookId(organizer.facebookId)
//    val phoneNumbers = Utilities.phoneNumbersSetToOptionString(Utilities.phoneNumbersStringToSet(organizer.phone))
//    val description = Utilities.formatDescription(organizer.description)
//    }
//  }

  def findIdByFacebookId(facebookId: Option[String]): Future[Option[Long]] = {
    val query = organizers.filter(_.facebookId === facebookId).map(_.id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] = {
    val query = organizers
      .sortBy(_.geographicPoint <-> geographicPoint)
      .drop(numberToReturn)
      .take(offset)
    db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Organizer]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None =>
        Logger.info("Organizer.findNearCity: no city found with this name")
        Future { Seq.empty }
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }

  def saveWithEventRelation(organizer: Organizer, eventId: Long): Future[Organizer] = save(organizer) flatMap {
    case organizer: Organizer => saveEventRelation(EventOrganizerRelation(eventId, organizer.id.get)) map {
      case 1 =>
        organizer
      case _ =>
        Logger.error("Organizer.saveWithEventRelation: not exactly one row was updated by saveEventRelation")
        organizer
    }
  }

  def saveEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] =
    db.run(eventsOrganizers += eventOrganizerRelation)


  def deleteEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] = db.run(
    eventsOrganizers.filter(eventOrganizer =>
      eventOrganizer.eventId === eventOrganizerRelation.eventId &&
        eventOrganizer.organizerId === eventOrganizerRelation.organizerId)
    .delete)

  def delete(id: Long): Future[Int] = db.run(organizers.filter(_.id === id).delete)

  /*
     def followById(userId : UUID, organizerId : Long): Try[Option[Long]] = Try {
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

     def followByFacebookId(userId : UUID, facebookId: String): Try[Option[Long]] = {
       DB.withConnection { implicit connection =>
         SQL("""SELECT organizerId FROM organizers WHERE facebookId = {facebookId}""")
           .on('facebookId -> facebookId)
           .as(scalar[Long].singleOpt) match {
           case None => throw new ThereIsNoOrganizerForThisFacebookIdException("Organizer.followOrganizerIdByFacebookId")
           case Some(organizerId) => followById(userId, organizerId)
         }
       }
     }

     def unfollowByOrganizerId(userId: UUID, organizerId: Long): Try[Int] = Try {
       DB.withConnection { implicit connection =>
         SQL(
           """DELETE FROM organizersFollowed
             | WHERE userId = {userId} AND organizerId = {organizerId}""".stripMargin)
           .on('userId -> userId,
             'organizerId -> organizerId)
           .executeUpdate()
       }
     }

     def isFollowed(userId: UUID, organizerId: Long): Boolean = try {
       DB.withConnection { implicit connection =>
         SQL(
           """SELECT exists(SELECT 1 FROM organizersFollowed
             |  WHERE userId = {userId} AND organizerId = {organizerId})""".stripMargin)
           .on("userId" -> userId,
             "organizerId" -> organizerId)
           .as(scalar[Boolean].single)
       }
     } catch {
       case e: Exception => throw new DAOException("Organizer.isOrganizerFollowed: " + e.getMessage)
     }

     def getFollowedOrganizers(userId: UUID): Seq[Organizer] = try {
       DB.withConnection { implicit connection =>
         SQL("""select a.* from organizers a
               |  INNER JOIN organizersFollowed af ON a.organizerId = af.organizerId
               |WHERE af.userId = {userId}""".stripMargin)
           .on('userId -> userId)
           .as(OrganizerParser.*)
   //        .map(getOrganizerProperties)
       }
     } catch {
       case e: Exception => throw new DAOException("Organizer.getFollowedOrganizers: " + e.getMessage)
     }
*/
  def readOrganizer(organizer: WSResponse, organizerId: String): Option[OrganizerWithAddress] = {
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
     OrganizerWithAddress(Organizer(None, Some(organizerId), name, utilities.formatDescription(description), None, phone, public_transit,
       website, verified = false, imagePath = source, geographicPoint = None), address = Option(Address(None, None,
         city, zip, street)))
     )
   organizer.json.asOpt[OrganizerWithAddress](readOrganizer)
  }

  def getOrganizerInfo(maybeOrganizerFacebookId: Option[String]): Future[Option[OrganizerWithAddress]] = maybeOrganizerFacebookId match {
    case None => Future { None }
    case Some(organizerId) =>
      WS.url("https://graph.facebook.com/"+ utilities.facebookApiVersion +"/" + organizerId)
        .withQueryString(
          "fields" -> "name,description,cover{source},location,phone,public_transit,website",
          "access_token" -> utilities.facebookToken)
        .get()
        .map { response => readOrganizer(response, organizerId) }
  }
}