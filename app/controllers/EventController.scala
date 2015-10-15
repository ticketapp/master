package controllers

import json.JsonHelper._
import models.{Address, Event, Tariff}
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.util.{Try, Failure, Success}
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}

class EventController @Inject()(ws: WSClient,
                                val messagesApi: MessagesApi,
                                val utilities: Utilities,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry,
                                val eventMethods: EventMethods)
  extends Silhouette[User, CookieAuthenticator] {

  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def events(offset: Int, numberToReturn: Int, geographicPoint: String) = Action.async {
    utilities.stringToGeographicPoint(geographicPoint) match {
      case Failure(exception) =>
        Logger.error("EventController.events: ", exception)
        Future { BadRequest(Json.toJson("Invalid geographicPoint")) }
      case Success(point) =>
        eventMethods.findNear(point, numberToReturn: Int, offset: Int) map { events =>
          Ok(Json.toJson(events))
        } recover { case t: Throwable =>
          Logger.error("EventController.events: ", t)
          InternalServerError("EventController.events: " + t.getMessage)
        }
    }
  }

  def eventsInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int) = Action.async {
    utilities.stringToGeographicPoint(geographicPoint) match {
      case Success(point) =>
        eventMethods.findInPeriodNear(hourInterval, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("Invalid geographicPoint"))
    }
  }

  def eventsPassedInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int)
  = Action.async {
    utilities.stringToGeographicPoint(geographicPoint) match {
      case Success(point) =>
        eventMethods.findPassedInHourIntervalNear(hourInterval, geographicPoint, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("EventController.eventsPassedInHourInterval: Invalid geographic point"))
    }
  }

  def find(id: Long) = Action.async {
    eventMethods.find(id) map {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }

  def findByPlace(placeId: Long) = Action.async {
    eventMethods.findAllByPlace(placeId) map { events =>
      Ok(Json.toJson(events))
    }
  }

  def findPassedByPlace(placeId: Long) = Action.async {
    eventMethods.findAllPassedByPlace(placeId) map { events =>
      Ok(Json.toJson(events))
    }
  }

  def findByOrganizer(organizerId: Long) = Action.async {
    eventMethods.findAllByOrganizer(organizerId) map { events =>
    Ok(Json.toJson(events)) }
  }

  def findPassedByOrganizer(organizerId: Long) = Action.async {
    eventMethods.findAllPassedByOrganizer(organizerId) map { events =>
      Ok(Json.toJson(events)) }
  }

  def findByArtist(facebookUrl: String) = Action.async {
    eventMethods.findAllByArtist(facebookUrl) map { events =>
      Ok(Json.toJson(events)) }
  }

  def findPassedByArtist(artistId: Long) = Action.async {
    eventMethods.findAllPassedByArtist(artistId) map { events =>
      Ok(Json.toJson(events)) }
  }

  def findByGenre(genre: String, geographicPointString: String, offset: Int , numberToReturn: Int) = Action.async {
    utilities.stringToGeographicPoint(geographicPointString) match {
      case Success(point) =>
        eventMethods.findAllByGenre(genre, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("EventController.findByGenre: Invalid geographic point"))
    }
  }

  def findAllContaining(pattern: String, center: String) = Action.async {
    utilities.stringToGeographicPoint(center) match {
      case Success(point) =>
        eventMethods.findAllContaining(pattern, point) map { events =>
          Ok(Json.toJson(events)) }
      case _ =>
        Future(BadRequest("EventController.findAllContaining: Invalid geographic point"))
    }
  }

  def findByCityPattern(pattern: String) = Action.async {
    eventMethods.findAllByCityPattern(pattern) map { events =>
      Ok(Json.toJson(events)) }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) =  Action.async {
    eventMethods.findNearCity(city, numberToReturn, offset) map { events =>
      Ok(Json.toJson(events)) }
  }

//  val eventBindingForm = Form(
//    mapping(
//      "name" -> nonEmptyText(2),
//      "geographicPoint" -> optional(nonEmptyText(3)),
//      "description" -> optional(nonEmptyText(2)),
//      "startTime" -> date("yyyy-MM-dd HH:mm"),
//      "endTime" -> optional(date("yyyy-MM-dd HH:mm")),
//      "ageRestriction" -> number,
//      "imagePath" -> optional(nonEmptyText(2)),
//      "tariffRange" -> optional(nonEmptyText(3)),
//      "ticketSellers" -> optional(nonEmptyText(3)),
//      "tariffs" -> list(
//        mapping(
//          "denomination" -> nonEmptyText,
//          "nbTicketToSell" -> number,
//          "price" -> bigDecimal,
//          "startTime" -> date("yyyy-MM-dd HH:mm"),
//          "endTime" -> date("yyyy-MM-dd HH:mm")
//        )(Tariff.formApply)(Tariff.formUnapply)),
//      "addresses" -> list(
//        mapping(
//          "city" -> optional(text(2)),
//          "zip" -> optional(text(2)),
//          "street" -> optional(text(2))
//        )(Address.formApply)(Address.formUnapply))
//    )(eventMethods.formApply)(eventMethods.formUnapply)
//  )
//
//  def createEvent = Action { implicit request =>
//    eventBindingForm.bindFromRequest().fold(
//      formWithErrors => {
//        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
//        BadRequest(formWithErrors.errorsAsJson)
//      },
//      event =>
//        eventMethods.save(event) match {
//          case Some(eventId) => Ok(Json.toJson(eventMethods.find(eventId)))
//          case None => InternalServerError
//        }
//    )
//  }
//
//  def followEvent(eventId : Long) = SecuredAction { implicit request =>
//    eventMethods.follow(request.identity.UUID, eventId) match {
//      case Success(_) =>
//        Created
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//        Logger.error(s"EventController.followEventByEventId: there is no event with the id $eventId")
//        Conflict
//      case Failure(unknownException) =>
//        Logger.error("EventController.followEvent", unknownException)
//        InternalServerError
//    }
//  }
//
//  def unfollowEvent(eventId : Long) = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    eventMethods.unfollow(userId, eventId) match {
//      case Success(1) =>
//        Ok
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//        Logger.error(s"The user (id: $userId) does not follow the event (eventId: $eventId) or the event does not exist.")
//        Conflict
//      case Failure(unknownException) =>
//        Logger.error("EventController.unfollowEvent", unknownException)
//        InternalServerError
//    }
//  }
//
//  def getFollowedEvents = SecuredAction { implicit request =>
//      Ok(Json.toJson(eventMethods.getFollowedEvents(request.identity.UUID)))
//  }
//
//  def isEventFollowed(eventId: Long) = SecuredAction { implicit request =>
//    Ok(Json.toJson(eventMethods.isFollowed(request.identity.UUID, eventId)))
//  }

  def createEventByFacebookId(facebookId: String) = Action.async {
    eventMethods.saveFacebookEventByFacebookId(facebookId) map { event =>
      Ok(Json.toJson(event))
    }
  }
}