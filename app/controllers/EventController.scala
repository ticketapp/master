package controllers

import json.JsonHelper._
import models.{Address, Event, Tariff}
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import securesocial.core.Identity
import services.Utilities._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object EventController extends Controller with securesocial.core.SecureSocial {
  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def events(offset: Int, numberToReturn: Int, geographicPoint: String) = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(Event.findNear(geographicPoint, numberToReturn: Int, offset: Int)))
      case _ =>
        Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def eventsInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(
          Event.findInHourIntervalNear(hourInterval, geographicPoint, offset, numberToReturn)))
      case _ =>
        BadRequest("Invalid geographicPoint")
    }
  }

  def eventsPassedInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int)
  = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(
          Event.findPassedInHourIntervalNear(hourInterval, geographicPoint, offset, numberToReturn)))
      case _ =>
        BadRequest
    }
  }

  def find(id: Long) = Action {
    Event.find(id) match {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }

  def findByPlace(placeId: Long) = Action { Ok(Json.toJson(Event.findAllByPlace(placeId))) }
  
  def findPassedByPlace(placeId: Long) = Action { Ok(Json.toJson(Event.findAllPassedByPlace(placeId))) }

  def findByOrganizer(organizerId: Long) = Action { Ok(Json.toJson(Event.findAllByOrganizer(organizerId))) }
  
  def findPassedByOrganizer(organizerId: Long) = Action { Ok(Json.toJson(Event.findAllPassedByOrganizer(organizerId))) }

  def findByArtist(facebookUrl: String) = Action { Ok(Json.toJson(Event.findAllByArtist(facebookUrl))) }
  
  def findPassedByArtist(artistId: Long) = Action { Ok(Json.toJson(Event.findAllPassedByArtist(artistId))) }

  def findByGenre(genre: String, geographicPointString: String, offset: Int , numberToReturn: Int) = Action {
    try {
      Event.findAllByGenre(genre, GeographicPoint(geographicPointString), offset, numberToReturn) match {
        case Success(events) =>
          Ok(Json.toJson(events))
        case Failure(throwable) =>
          Logger.error("EventController.eventsByGenre:", throwable)
          InternalServerError
      }
    } catch {
      case e: IllegalArgumentException => BadRequest("GeographicPoint wrongly formatted")
    }
  }

  def findAllContaining(pattern: String, center: String) = Action {
    Ok(Json.toJson(Event.findAllContaining(pattern, center)))
  }

  def findByCityPattern(pattern: String) = Action {
    Ok(Json.toJson(Event.findAllByCityPattern(pattern)))
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Event.findNearCity(city, numberToReturn, offset)))
  }

  val eventBindingForm = Form(
    mapping(
      "name" -> nonEmptyText(2),
      "geographicPoint" -> optional(nonEmptyText(3)),
      "description" -> optional(nonEmptyText(2)),
      "startTime" -> date("yyyy-MM-dd HH:mm"),
      "endTime" -> optional(date("yyyy-MM-dd HH:mm")),
      "ageRestriction" -> number,
      "imagePath" -> optional(nonEmptyText(2)),
      "tariffRange" -> optional(nonEmptyText(3)),
      "ticketSellers" -> optional(nonEmptyText(3)),
      "tariffs" -> list(
        mapping(
          "denomination" -> nonEmptyText,
          "nbTicketToSell" -> number,
          "price" -> bigDecimal,
          "startTime" -> date("yyyy-MM-dd HH:mm"),
          "endTime" -> date("yyyy-MM-dd HH:mm")
        )(Tariff.formApply)(Tariff.formUnapply)),
      "addresses" -> list(
        mapping(
          "city" -> optional(text(2)),
          "zip" -> optional(text(2)),
          "street" -> optional(text(2))
        )(Address.formApply)(Address.formUnapply))
    )(Event.formApply)(Event.formUnapply)
  )

  def createEvent = Action { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      event =>
        Event.save(event) match {
          case Some(eventId) => Ok(Json.toJson(Event.find(eventId)))
          case None => InternalServerError
        }
    )
  }

  def followEvent(eventId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Event.follow(request.user.identityId.userId, eventId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"EventController.followEventByEventId: there is no event with the id $eventId")
        Conflict
      case Failure(unknownException) =>
        Logger.error("EventController.followEvent", unknownException)
        InternalServerError
    }
  }

  def unfollowEvent(eventId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Event.unfollow(userId, eventId) match {
      case Success(1) =>
        Ok
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the event (eventId: $eventId) or the event does not exist.")
        Conflict
      case Failure(unknownException) =>
        Logger.error("EventController.unfollowEvent", unknownException)
        InternalServerError
    }
  }

  def getFollowedEvents = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Event.getFollowedEvents(identity.identityId)))
    }
  }

  def isEventFollowed(eventId: Long) = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Event.isFollowed(identity.identityId, eventId)))
    }
  }

  def createEventByFacebookId(facebookId: String) = Action.async {
    Event.saveFacebookEventByFacebookId(facebookId) match {
      case Success(eventuallyMaybeId) =>
        eventuallyMaybeId map { maybeId => Ok(Json.toJson(maybeId)) }
      case Failure(exception) =>
        Logger.error("EventController.createEventByFacebookId: event could not be saved")
        Future { InternalServerError("EventController.createEventByFacebookId: event could not be saved") }
    }
  }
}