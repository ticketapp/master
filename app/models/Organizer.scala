package models

import java.util.UUID
import javax.inject.Inject

import com.vividsolutions.jts.geom.Geometry
import controllers.ThereIsNoOrganizerForThisFacebookIdException
import json.JsonHelper._
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{FollowService, MyPostgresDriver, Utilities}

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

class OrganizerMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                 val placeMethods: PlaceMethods,
                                 val addressMethods: AddressMethods,
                                 val utilities: Utilities,
                                 val geographicPointMethods: SearchGeographicPoint)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with MyDBTableDefinitions {

  def find(numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] = {
    val tupledJoin = organizers joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result) map(_ map OrganizerWithAddress.tupled)
  }

  // not used
  def findAllByEventId(eventId: Long): Future[Seq[OrganizerWithAddress]] = {
    val query = for {
      eventOrganizer <- eventsOrganizers.filter(_.eventId === eventId)
      organizerWithAddress <- organizers joinLeft addresses on (_.addressId === _.id)
      if organizerWithAddress._1.id === eventOrganizer.organizerId
    } yield organizerWithAddress

    db.run(query.result) map(_ map OrganizerWithAddress.tupled)

  }
  //

  def findById(id: Long): Future[Option[OrganizerWithAddress]] = {
    val query = organizers.filter(_.id === id) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result.headOption).map {
      case Some(organizer) => Option(OrganizerWithAddress.tupled(organizer))
      case None => None
    }
  }

  def findAllContaining(pattern: String): Future[Seq[OrganizerWithAddress]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      organizerWithAddress <- organizers joinLeft addresses on (_.addressId === _.id)
      if organizerWithAddress._1.name.toLowerCase like s"%$lowercasePattern%"
    } yield organizerWithAddress
    db.run(query.result) map(_ map OrganizerWithAddress.tupled)
  }

  def saveWithAddress(organizerWithAddress: OrganizerWithAddress): Future[OrganizerWithAddress] = {
    organizerWithAddress.address match {
      case Some(address) => 
        addressMethods.save(address) flatMap { savedAddress =>
          save(organizerWithAddress.organizer.copy(addressId = savedAddress.id)) map { orga =>
            OrganizerWithAddress(orga, Option(savedAddress))
          }
        }
      case None =>
        save(organizerWithAddress.organizer) map { savedOrganizer =>
          OrganizerWithAddress(savedOrganizer, None)
        }
    }
  }
  
  def save(organizer: Organizer): Future[Organizer] = {
    val organizerWithFormattedDescription = organizer.copy(
      description = utilities.formatDescription(organizer.description))
    val organizerWithFormattedDescriptionAndPhone = organizerWithFormattedDescription.copy( phone =
      utilities.phoneNumbersSetToOptionString(utilities.phoneNumbersStringToSet(organizer.phone)))
    organizer.facebookId match {
      case None =>
        doSave(organizerWithFormattedDescriptionAndPhone)
      case Some(facebookId) =>
        placeMethods.findIdByFacebookId(facebookId) flatMap { maybePlaceId =>
          val organizerWithLinkedPlace = organizerWithFormattedDescriptionAndPhone.copy(linkedPlaceId = maybePlaceId)
          doSave(organizerWithLinkedPlace)
        }
    }
  }

  def doSave(organizer: Organizer): Future[Organizer] = {
    db.run((for {
      organizerFound <- organizers.filter(_.facebookId === organizer.facebookId).result.headOption
      result <- organizerFound.map(DBIO.successful).getOrElse(organizers returning organizers.map(_.id) += organizer)
    } yield result match {
        case o: Organizer => o
        case id: Long => organizer.copy(id = Option(id))
      }).transactionally)
  }

  def findIdByFacebookId(facebookId: Option[String]): Future[Option[Long]] = {
    val query = organizers.filter(_.facebookId === facebookId).map(_.id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] = {
    val query = for {
      organizerWithAddress <- organizers joinLeft addresses on (_.addressId === _.id)
    } yield organizerWithAddress

    db.run(query
      .sortBy(_._1.geographicPoint <-> geographicPoint)
      .drop(offset)
      .take(numberToReturn)
      .result) map(_ map OrganizerWithAddress.tupled)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None =>
        Logger.info("Organizer.findNearCity: no city found with this name")
        Future { Seq.empty }
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }

  // not used
  def saveWithEventRelation(organizer: Organizer, eventId: Long): Future[Organizer] = save(organizer) flatMap { organizer =>
    saveEventRelation(EventOrganizerRelation(eventId, organizer.id.get)) map {
      case 1 =>
        organizer
      case _ =>
        Logger.error("Organizer.saveWithEventRelation: not exactly one row was updated by saveEventRelation")
        organizer
    }
  }
  //

  def saveEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] =
    db.run(eventsOrganizers += eventOrganizerRelation)


  def deleteEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] = db.run(
    eventsOrganizers.filter(eventOrganizer =>
      eventOrganizer.eventId === eventOrganizerRelation.eventId &&
        eventOrganizer.organizerId === eventOrganizerRelation.organizerId)
    .delete)

  def delete(id: Long): Future[Int] = db.run(organizers.filter(_.id === id).delete)

  def followByFacebookId(userId : UUID, facebookId: String): Future[Int] = findIdByFacebookId(Some(facebookId)) flatMap {
    case None =>
      Logger.error("Organizer.followByFacebookId: ", ThereIsNoOrganizerForThisFacebookIdException("Organizer.followByFacebookId"))
      Future { 0 }
    case Some(organizerId) =>
      followByOrganizerId(UserOrganizerRelation(userId, organizerId))
  }

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