package placesDomain

import java.util.UUID
import javax.inject.Inject

import addresses.{SearchGeographicPoint, Address, AddressMethods}
import com.vividsolutions.jts.geom.Geometry
import database.{MyPostgresDriver, UserPlaceRelation, EventPlaceRelation, MyDBTableDefinitions}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import MyPostgresDriver.api._
import services.{FollowService, Utilities}
import silhouette.DBTableDefinitions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class Place(id: Option[Long] = None,
                 name: String,
                 facebookId: Option[String] = None,
                 geographicPoint: Option[Geometry] = None,
                 description: Option[String] = None,
                 websites: Option[String] = None,
                 capacity: Option[Int] = None,
                 openingHours: Option[String] = None,
                 imagePath: Option[String] = None,
                 addressId: Option[Long] = None,
                 linkedOrganizerId: Option[Long] = None)

case class PlaceWithAddress(place: Place, address: Option[Address] = None)

class PlaceMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val geographicPointMethods: SearchGeographicPoint,
                             val addressMethods: AddressMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with DBTableDefinitions
    with MyDBTableDefinitions
    with Utilities {

  def delete(id: Long): Future[Int] = db.run(places.filter(_.id === id).delete)

  def doSave(place: Place): Future[Place] =
    db.run((for {
      placeFound <- places.filter(_.facebookId === place.facebookId).result.headOption
      result <- placeFound.map(DBIO.successful).getOrElse(places returning places.map(_.id) += place)
    } yield result match {
        case p: Place => p
        case id: Long => place.copy(id = Option(id))
      }).transactionally)

  def save(place: Place): Future[Place] = {
    val placeWithFormattedDescription = place.copy(description = formatDescription(place.description))
    place.facebookId match {
      case Some(facebookId) =>
        findOrganizerIdByFacebookId(facebookId) flatMap { maybePlaceId =>
          val organizerWithLinkedPlace = placeWithFormattedDescription.copy(linkedOrganizerId = maybePlaceId)
          doSave(organizerWithLinkedPlace)
        }
      case _ =>
        doSave(placeWithFormattedDescription)
    }
  }

  def update(place: Place): Future[Int] = db.run(places.filter(_.id === place.id).update(place))

  def saveWithAddress(placeWithAddress: PlaceWithAddress): Future[PlaceWithAddress] = {
    placeWithAddress.address match {
      case Some(address) =>
        addressMethods.saveAddressWithGeoPoint(address) flatMap { savedAddress =>
          save(placeWithAddress.place.copy(
            addressId = savedAddress.id,
            geographicPoint = savedAddress.geographicPoint)) map { place =>
            PlaceWithAddress(place, Option(savedAddress))
          }
        }
      case None =>
        save(placeWithAddress.place) map { savedPlace =>
          PlaceWithAddress(savedPlace, None)
        }
    }
  }

  def findById(id: Long): Future[Option[PlaceWithAddress]] = {
    val tupledJoin = places.filter(_.id === id) joinLeft addresses on (_.addressId === _.id)

    db.run(tupledJoin.result.headOption) map(_ map PlaceWithAddress.tupled)
  }

  def findSinceOffset(offset: Long, numberToReturn: Long): Future[Seq[PlaceWithAddress]] = {
    val query = places.drop(offset).take(numberToReturn) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result) map(_ map PlaceWithAddress.tupled)
  }

  def findOrganizerIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(organizers.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def findIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(places.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def getPlaceByFacebookId(placeFacebookId : String) : Future[Option[PlaceWithAddress]] = findIdByFacebookId(placeFacebookId) flatMap {
    case Some(id) =>
      findById(id) flatMap {
        case Some(place) =>
          Future(Option(place))
        case None =>
          getPlaceOnFacebook(placeFacebookId)
      }
    case None =>
      getPlaceOnFacebook(placeFacebookId)
  }

  def getPlaceOnFacebook(placeFacebookId: String): Future[Option[PlaceWithAddress]] =
   WS.url("https://graph.facebook.com/" + facebookApiVersion + "/" + placeFacebookId)
     .withQueryString(
       "fields" -> "about,location,website,hours,cover,name",
       "access_token" -> facebookToken)
     .get()
     .flatMap(readFacebookPlace) recover {
     case NonFatal(exception) =>
       Logger.error("Place.getPlaceOnFacebook:\nMessage: ", exception)
       None
   }

  def placeRead: Reads[PlaceWithAddress] = (
    (__ \ "about").readNullable[String] and
      (__ \ "cover").readNullable[Option[String]](
        (__ \ "source").readNullable[String]
      ) and
      (__ \ "name").read[String] and
      (__ \ "id").readNullable[String] and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "street").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "zip").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "city").readNullable[String]
      ) and
      (__ \ "location").readNullable[Option[String]](
        (__ \ "country").readNullable[String]
      ) and
      (__ \ "website").readNullable[String]
    ).apply((about: Option[String], source: Option[Option[String]], name: String, facebookId: Option[String],
             street: Option[Option[String]], zip: Option[Option[String]], city: Option[Option[String]],
             country: Option[Option[String]], website: Option[String]) => {
      val address = Address(None, None, city.flatten, zip.flatten, street.flatten)
      PlaceWithAddress(
        place = Place(id = None, name = name, facebookId = facebookId, geographicPoint = None, description = about,
          websites = normalizeMaybeWebsite(website), capacity = None, openingHours = None, imagePath = source.flatten),
        address = Option(address))
  })

  def readPlaces(places: WSResponse): Seq[PlaceWithAddress] = {
    val collectOnlyPlaces: Reads[Seq[PlaceWithAddress]] = Reads.seq(placeRead) map(_.toVector)

    (places.json \ "data")
      .asOpt[Seq[PlaceWithAddress]](collectOnlyPlaces)
      .getOrElse(Seq.empty)
  }

  def readFacebookPlace (placeFacebookResponse: WSResponse): Future[Option[PlaceWithAddress]] = Try {
    placeFacebookResponse.json.as[PlaceWithAddress](placeRead)
  } match {
    case Success(placeWithAddress) =>
        saveWithAddress(placeWithAddress) map Option.apply
    case Failure(exception: IllegalArgumentException) =>
      Logger.info("Place.readFacebookPlace: address must contain at least one field")
      Future(None)
    case Failure(exception) =>
      Logger.error("Place.readFacebookPlace:\nMessage:", exception)
      Future(None)
  }

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[PlaceWithAddress]] = {
    val query = for {
      placeWithAddress <- places
        .sortBy(p => (p.geographicPoint <-> geographicPoint).desc)
        .drop(offset)
        .take(numberToReturn) joinLeft
        addresses on (_.addressId === _.id)
    } yield placeWithAddress

    db.run(query.result) map(_ map PlaceWithAddress.tupled)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[PlaceWithAddress]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None => Future(Seq.empty)
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }

  def findAllByEventId(eventId: Long): Future[Seq[PlaceWithAddress]] = {
    val query = for {
      eventPlace <- eventsPlaces.filter(_.eventId === eventId)
      placeWithAddress <- places joinLeft addresses on (_.addressId === _.id)
      if placeWithAddress._1.id === eventPlace.placeId
    } yield placeWithAddress

    db.run(query.result) map(_ map PlaceWithAddress.tupled)
  }

  def findAllContaining(pattern: String): Future[Seq[PlaceWithAddress]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = places
      .filter(place => place.name.toLowerCase like s"%$lowercasePattern%")
      .take(5) joinLeft addresses on (_.addressId === _.id)
    db.run(query.result)  map(_ map PlaceWithAddress.tupled)
  }

  def followByFacebookId(userId : UUID, facebookId: String): Future[Int] = findIdByFacebookId(facebookId) flatMap {
    case Some(placeId) =>
      followByPlaceId(UserPlaceRelation(userId, placeId))
    case None =>
      Logger.error("Place.followByFacebookId: ThereIsNoPlaceForThisFacebookId")
      Future(0)
  }

  def saveEventRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] = db.run(eventsPlaces += eventPlaceRelation)
 
  def deleteEventRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] = db.run(eventsPlaces
    .filter(eventPlace => eventPlace.eventId === eventPlaceRelation.eventId &&
      eventPlace.placeId === eventPlaceRelation.placeId)
    .delete)

  def saveEventRelations(eventPlaceRelations: Seq[EventPlaceRelation]): Future[Boolean] = db.run(
    eventsPlaces ++= eventPlaceRelations) map { _ =>
        true
    } recover {
    case e: Exception =>
      Logger.error("place.saveEventRelations: ", e)
      false
  }

  def saveWithEventRelation(place: PlaceWithAddress, eventId: Long): Future[PlaceWithAddress] = saveWithAddress(place) flatMap { savedPlace =>
    saveEventRelation(EventPlaceRelation(eventId, savedPlace.place.id.getOrElse(0))) map {
      case 1 =>
        savedPlace
      case _ =>
        Logger.error(s"Place.saveWithEventRelation: not exactly one row saved by Place.saveEventRelation for place $savedPlace and eventId $eventId")
        savedPlace
    }
  }
}
