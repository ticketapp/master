package models

import java.sql.Timestamp
import javax.inject.Inject

import com.vividsolutions.jts.geom.Point
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
import silhouette.DBTableDefinitions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class Place (id: Option[Long],
                  name: String,
                  facebookId: Option[String] = None,
                  geographicPoint: Option[Point],
                  description: Option[String] = None,
                  webSites: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  imagePath: Option[String] = None,
                  /*address : Option[Address] = None,*/
                  linkedOrganizerId: Option[Long] = None)

class PlaceMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val geographicPointMethods: GeographicPointMethods,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with DBTableDefinitions with MyDBTableDefinitions {

  def formApply(name: String, facebookId: Option[String], geographicPoint: Option[String], description: Option[String],
                webSite: Option[String], capacity: Option[Int], openingHours: Option[String],
                imagePath: Option[String], city: Option[String], zip: Option[String], street: Option[String]): Place = {
    val address = Option(Address(None, None, city, zip, street))
    val point = utilities.optionStringToOptionPoint(geographicPoint)

    Place(None, name, facebookId, point, description, webSite, capacity, openingHours, imagePath/*, address*/)
  }

  def formUnapply(place: Place) =
    Some((place.name, place.facebookId, place.geographicPoint, place.description, place.webSites, place.capacity,
      place.openingHours, place.imagePath/*, place.address.get.city, place.address.get.zip, place.address.get.street*/))

  def delete(id: Long): Future[Int] = db.run(places.filter(_.id === id).delete)

  def save(place: Place): Future[Place] = {
//      val placeuallyAddressId = Address.saveAddressInFutureWithGeoPoint(place.address)
//      'webSites -> Utilities.setToOptionString(getNormalizedWebsitesInText(place.webSites))
//    'organizerId -> organizerMethods.findIdByFacebookId(place.facebookId))
    val query = places returning places.map(_.id) into ((place, id) => place.copy(id = Option(id))) +=
      place.copy(description =  utilities.formatDescription(place.description))

    db.run(query)
  }

  def find(id: Long): Future[Option[Place]] = db.run(places.filter(_.id === id).result.headOption)

  def findIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(places.filter(_.facebookId === facebookId).map(_.id).result.headOption)

  def getPlaceByFacebookId(placeFacebookId : String) : Future[Place] = findIdByFacebookId(placeFacebookId) flatMap {
    case Some(id) =>
      find(id) flatMap {
        case Some(place) =>
          Future(place)
        case None =>
          getPlaceOnFacebook(placeFacebookId)
      }
    case None =>
      getPlaceOnFacebook(placeFacebookId)
  }

 def getPlaceOnFacebook(placeFacebookId: String): Future[Place] =
   WS.url("https://graph.facebook.com/" + utilities.facebookApiVersion + "/" + placeFacebookId)
     .withQueryString(
       "fields" -> "about,location,website,hours,cover,name",
       "access_token" -> utilities.facebookToken)
     .get()
     .flatMap { readFacebookPlace }

 def readFacebookPlace (placeFacebookResponse: WSResponse): Future[Place] = {
   val placeRead = (
     (__ \ "about").readNullable[String] and
       (__ \ "cover" \ "source").readNullable[String] and
       (__ \ "name").read[String] and
       (__ \ "id").readNullable[String] and
       (__ \ "location" \ "street").readNullable[String] and
       (__ \ "location" \ "zip").readNullable[String] and
       (__ \ "location" \ "city").readNullable[String] and
       (__ \ "location" \ "country").readNullable[String] and
       (__ \ "website").readNullable[String]
     ).apply((about: Option[String], source: Option[String], name: String, facebookId: Option[String],
              street: Option[String], zip: Option[String], city: Option[String], country: Option[String],
              website: Option[String]) => {
     val address = Address(None, None, city, zip, street)
     val newPlace = Place(None, name, facebookId, None, about, website, None, None, source/*, Option(address)*/)
     save(newPlace)
   })

   placeFacebookResponse.json.as[Future[Place]](placeRead)
 }

  def findNear(geographicPoint: Point, numberToReturn: Int, offset: Int): Future[Seq[Place]] = {
   val query = places
     .sortBy(_.geographicPoint <-> geographicPoint)
     .drop(numberToReturn)
     .take(offset)
   db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Place]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None => Future(Seq.empty)
      case Some(geographicPoint) => findNear(geographicPoint, numberToReturn, offset)
  }

  def findAll: Future[Seq[Place]] = db.run(places.result)

  def findAllByEvent(eventId: Long): Future[Seq[Place]] = {
    val query = for {
      event <- events if event.id === eventId
      eventsPlaces <- eventsPlaces
      place <- places if place.id === eventsPlaces.placeId
    } yield place

    db.run(query.result)
  }

  def findAllWithNonEmptyFacebookId: Future[Seq[Place]] = db.run(places.filter(_.facebookId.nonEmpty).result)

  def findAllContaining(pattern: String): Future[Seq[Place]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = places.filter(place => place.name.toLowerCase like s"%$lowercasePattern%").take(5)
    db.run(query.result)
  }

  def followByPlaceId(placeRelation: UserPlaceRelation): Future[Int] = db.run(placesFollowed += placeRelation)

  def unfollowByPlaceId(userPlaceRelation: UserPlaceRelation): Future[Int] = db.run(
    placesFollowed
      .filter(placeFollowed =>
      placeFollowed.userId === userPlaceRelation.userId && placeFollowed.placeId === userPlaceRelation.placeId)
      .delete)

  def followByFacebookId(userId : String, facebookId: String): Future[Int] =
   findIdByFacebookId(facebookId) flatMap {
     case Some(placeId) =>
       followByPlaceId(UserPlaceRelation(userId, placeId))
     case None =>
       Logger.error("Place.followByFacebookId: ThereIsNoPlaceForThisFacebookId")
       Future(0)
   }

  def isFollowed(userPlaceRelation: UserPlaceRelation): Future[Boolean] = {
  val query =
    sql"""SELECT exists(
         |  SELECT 1 FROM placesFollowed WHERE userId = ${userPlaceRelation.userId} AND placeId = ${userPlaceRelation.placeId})"""
    .as[Boolean]
  db.run(query.head)
  }

  def getFollowed(userId: String): Future[Seq[Place] ]= {
    val query = for {
      placeFollowed <- placesFollowed if placeFollowed.userId === userId
      place <- places if place.id === placeFollowed.placeId
    } yield place

    db.run(query.result)
  }

  def saveEventRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] = db.run(eventsPlaces += eventPlaceRelation)
 
  def deletePlaceRelation(eventPlaceRelation: EventPlaceRelation): Future[Int] = db.run(eventsPlaces.filter(event =>
    event.eventId === eventPlaceRelation.eventId && event.placeId === eventPlaceRelation.placeId).delete)
}
