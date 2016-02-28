package organizersDomain

import java.util.UUID
import javax.inject.Inject

import addresses.{SearchGeographicPoint, Address, AddressMethods}
import application.ThereIsNoOrganizerForThisFacebookIdException
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Geometry}
import database.{MyPostgresDriver, EventOrganizerRelation, UserOrganizerRelation, MyDBTableDefinitions}
import placesDomain.PlaceMethods
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import MyPostgresDriver.api._
import services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

case class Organizer (id: Option[Long] = None,
                      facebookId: Option[String] = None,
                      name: String,
                      description: Option[String] = None,
                      addressId: Option[Long] = None,
                      phone: Option[String] = None,
                      publicTransit: Option[String] = None,
                      websites: Option[String] = None,
                      verified: Boolean = false,
                      imagePath: Option[String] = None,
                      geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                      linkedPlaceId: Option[Long] = None)

case class OrganizerWithAddress(organizer: Organizer, address: Option[Address] = None) extends SortableByGeographicPoint {
  val geographicPoint = organizer.geographicPoint
}


class OrganizerMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                 val placeMethods: PlaceMethods,
                                 val addressMethods: AddressMethods,
                                 val geographicPointMethods: SearchGeographicPoint)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with MyDBTableDefinitions
    with Utilities
    with SortByDistanceToPoint
    with LoggerHelper {

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[OrganizerWithAddress]] = {
    val tupledJoin = organizers.drop(offset).take(numberToReturn) joinLeft addresses on (_.addressId === _.id)

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
        addressMethods.saveAddressWithGeoPoint(address) flatMap { savedAddress =>
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
    val eventuallyMaybePlaceId: Future[Option[Long]] = organizer.facebookId match {
      case None =>
        Future(None)
      case Some(facebookId) =>
        placeMethods.findIdByFacebookId(facebookId)
    }

    eventuallyMaybePlaceId flatMap { maybePlaceId =>
      doSave(organizer.copy(
        description = formatDescription(organizer.description),
        phone = phoneNumbersSetToOptionString(phoneNumbersStringToSet(organizer.phone)),
        linkedPlaceId = maybePlaceId
      ))
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

  def update(organizer: Organizer): Future[Int] = db.run(organizers.filter(_.id === organizer.id).update(organizer))

  def findIdByFacebookId(facebookId: Option[String]): Future[Option[Long]] = {
    val query = organizers.filter(_.facebookId === facebookId).map(_.id)
    db.run(query.result.headOption)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] = {
    val query = for {
      organizerWithAddress <- organizers
        .sortBy(organizer => (organizer.geographicPoint <-> geographicPoint, organizer.id))
        .drop(offset)
        .take(numberToReturn) joinLeft addresses on (_.addressId === _.id)
    } yield organizerWithAddress

    db.run(query.result) map(_ map OrganizerWithAddress.tupled) map(sortByDistanceToPoint(geographicPoint, _))
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[OrganizerWithAddress]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None =>
        Logger.info("Organizer.findNearCity: no city found with this name")
        Future { Seq.empty }
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }

  def saveWithEventRelation(organizer: OrganizerWithAddress, eventId: Long): Future[OrganizerWithAddress] =
    saveWithAddress(organizer) flatMap { savedOrganizer =>
    saveEventRelation(EventOrganizerRelation(eventId, savedOrganizer.organizer.id.get)) map {
      case 1 =>
        savedOrganizer
      case _ =>
        Logger.error("Organizer.saveWithEventRelation: not exactly one row was updated by saveEventRelation")
        savedOrganizer
    }
  }

  def saveEventRelation(eventOrganizerRelation: EventOrganizerRelation): Future[Int] =
    db.run(eventsOrganizers += eventOrganizerRelation) recover { case NonFatal(e) =>
      log(s"The relation $eventOrganizerRelation was not saved", e)
      0
    }

  def saveEventRelations(eventOrganizerRelations: Seq[EventOrganizerRelation]): Future[Boolean] =
    db.run(eventsOrganizers ++= eventOrganizerRelations) map { _ =>
      true
    } recover {
      case e: Exception =>
        Logger.error("Organizer.saveEventRelations: ", e)
        false
    }

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

  def organizerRead: Reads[OrganizerWithAddress] = (
    (__ \ "name").read[String] and
      (__ \ "id").readNullable[String] and
      (__ \ "description").readNullable[String] and
      (__ \ "cover" \ "source").readNullable[String] and
      (__ \ "location" \ "street").readNullable[String] and
      (__ \ "location" \ "zip").readNullable[String] and
      (__ \ "location" \ "city").readNullable[String] and
      (__ \ "phone").readNullable[String] and
      (__ \ "public_transit").readNullable[String] and
      (__ \ "website").readNullable[String])
    .apply((name: String, facebookId: Option[String], description: Option[String], source: Option[String], street: Option[String],
            zip: Option[String], city: Option[String], phone: Option[String], public_transit: Option[String],
            website: Option[String]) =>
      OrganizerWithAddress(organizer = Organizer(id = None, facebookId = facebookId, name = name,
        description = formatDescription(description), addressId = None, phone = phone, publicTransit = public_transit,
        websites = website, verified = false, imagePath = source),
        address = Option(Address(id = None, city = city, zip = zip, street = street)))
    )
  
  def jsonToOrganizer(organizer: JsValue): Option[OrganizerWithAddress] = organizer.asOpt[OrganizerWithAddress](organizerRead)
  

  def readOrganizers(organizers: WSResponse): Seq[OrganizerWithAddress] = {
    val collectOnlyOrganizers: Reads[Seq[OrganizerWithAddress]] = Reads.seq(organizerRead) map(_.toVector)

    (organizers.json \ "data")
      .asOpt[Seq[OrganizerWithAddress]](collectOnlyOrganizers)
      .getOrElse(Seq.empty)
  }

  def getOrganizerInfo(maybeOrganizerFacebookId: Option[String]): Future[Option[OrganizerWithAddress]] = maybeOrganizerFacebookId match {
    case None => Future(None)
    case Some(organizerId) =>
      WS.url("https://graph.facebook.com/"+ facebookApiVersion +"/" + organizerId)
        .withQueryString(
          "fields" -> "name,description,cover{source},location,phone,public_transit,website",
          "access_token" -> facebookToken)
        .get()
        .map(response => jsonToOrganizer(response.json))
  }
}