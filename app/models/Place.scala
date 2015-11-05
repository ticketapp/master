package models

import java.util.UUID
import javax.inject.Inject

import com.vividsolutions.jts.geom.Geometry
import json.JsonHelper._
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.MyPostgresDriver.api._
import services.{FollowService, MyPostgresDriver, Utilities}
import silhouette.DBTableDefinitions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class Place (id: Option[Long] = None,
                  name: String,
                  facebookId: Option[String] = None,
                  geographicPoint: Option[Geometry] = None,
                  description: Option[String] = None,
                  webSites: Option[String] = None,
                  capacity: Option[Int] = None,
                  openingHours: Option[String] = None,
                  imagePath: Option[String] = None,
                  addressId: Option[Long] = None,
                  linkedOrganizerId: Option[Long] = None)

class PlaceMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val geographicPointMethods: SearchGeographicPoint,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with FollowService
    with DBTableDefinitions
    with MyDBTableDefinitions {

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
    val placeWithFormattedDescription = place.copy(description = utilities.formatDescription(place.description))
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

  def find(id: Long): Future[Option[Place]] = db.run(places.filter(_.id === id).result.headOption)

  def findOrganizerIdByFacebookId(facebookId: String): Future[Option[Long]] =
    db.run(organizers.filter(_.facebookId === facebookId).map(_.id).result.headOption)

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

  def findNear(geographicPoint: Geometry, numberToReturn: Int, offset: Int): Future[Seq[Place]] = {
    val query = places
     .sortBy(_.geographicPoint <-> geographicPoint)
     .drop(offset)
     .take(numberToReturn)
    db.run(query.result)
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int): Future[Seq[Place]] =
    geographicPointMethods.findGeographicPointOfCity(city) flatMap {
      case None => Future(Seq.empty)
      case Some(geographicPoint) =>
        findNear(geographicPoint, numberToReturn, offset)
  }

  def findAllByEvent(eventId: Long): Future[Seq[Place]] = {
    val query = for {
      event <- events if event.id === eventId
      eventsPlaces <- eventsPlaces
      place <- places if place.id === eventsPlaces.placeId
    } yield place

    db.run(query.result)
  }

  def findAllContaining(pattern: String): Future[Seq[Place]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = places
      .filter(place => place.name.toLowerCase like s"%$lowercasePattern%")
      .take(5)
    db.run(query.result)
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

  def saveWithEventRelation(place: Place, eventId: Long): Future[Place] = save(place) flatMap { savedPlace =>
    saveEventRelation(EventPlaceRelation(eventId, savedPlace.id.getOrElse(0))) map {
      case 1 =>
        savedPlace
      case _ =>
        Logger.error(s"Place.saveWithEventRelation: not exactly one row saved by Place.saveEventRelation for place $savedPlace and eventId $eventId")
        savedPlace
    }
  }
}
