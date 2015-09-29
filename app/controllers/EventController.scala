package controllers

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
import services.Utilities._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

class EventController @Inject()(ws: WSClient,
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry,
                                val eventMethods: EventMethods)
  extends Silhouette[User, CookieAuthenticator] {

  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def events(offset: Int, numberToReturn: Int, geographicPoint: String) = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(eventMethods.findNear(geographicPoint, numberToReturn: Int, offset: Int)))
      case _ =>
        Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def eventsInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(
          eventMethods.findInHourIntervalNear(hourInterval, geographicPoint, offset, numberToReturn)))
      case _ =>
        BadRequest("Invalid geographicPoint")
    }
  }

  def eventsPassedInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int)
  = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(
          eventMethods.findPassedInHourIntervalNear(hourInterval, geographicPoint, offset, numberToReturn)))
      case _ =>
        BadRequest
    }
  }

  def find(id: Long) = Action {
    eventMethods.find(id) match {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }

  def findByPlace(placeId: Long) = Action { Ok(Json.toJson(eventMethods.findAllByPlace(placeId))) }
  
  def findPassedByPlace(placeId: Long) = Action { Ok(Json.toJson(eventMethods.findAllPassedByPlace(placeId))) }

  def findByOrganizer(organizerId: Long) = Action { Ok(Json.toJson(eventMethods.findAllByOrganizer(organizerId))) }
  
  def findPassedByOrganizer(organizerId: Long) = Action { Ok(Json.toJson(eventMethods.findAllPassedByOrganizer(organizerId))) }

  def findByArtist(facebookUrl: String) = Action { Ok(Json.toJson(eventMethods.findAllByArtist(facebookUrl))) }
  
  def findPassedByArtist(artistId: Long) = Action { Ok(Json.toJson(eventMethods.findAllPassedByArtist(artistId))) }

  def findByGenre(genre: String, geographicPointString: String, offset: Int , numberToReturn: Int) = Action {
    try {
      eventMethods.findAllByGenre(genre, GeographicPoint(geographicPointString), offset, numberToReturn) match {
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
    Ok(Json.toJson(eventMethods.findAllContaining(pattern, center)))
  }

  def findByCityPattern(pattern: String) = Action {
    Ok(Json.toJson(eventMethods.findAllByCityPattern(pattern)))
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(eventMethods.findNearCity(city, numberToReturn, offset)))
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
    )(eventMethods.formApply)(eventMethods.formUnapply)
  )

  def createEvent = Action { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      event =>
        eventMethods.save(event) match {
          case Some(eventId) => Ok(Json.toJson(eventMethods.find(eventId)))
          case None => InternalServerError
        }
    )
  }

  def followEvent(eventId : Long) = SecuredAction { implicit request =>
    eventMethods.follow(request.identity.UUID, eventId) match {
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

  def unfollowEvent(eventId : Long) = SecuredAction { implicit request =>
    val userId = request.identity.UUID
    eventMethods.unfollow(userId, eventId) match {
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

  def getFollowedEvents = SecuredAction { implicit request =>
      Ok(Json.toJson(eventMethods.getFollowedEvents(request.identity.UUID)))
  }

  def isEventFollowed(eventId: Long) = SecuredAction { implicit request =>
    Ok(Json.toJson(eventMethods.isFollowed(request.identity.UUID, eventId)))
  }

  def createEventByFacebookId(facebookId: String) = Action.async {
    eventMethods.saveFacebookEventByFacebookId(facebookId) map {
      case Some(res: Long) =>
        Ok(Json.toJson(res))
      case None =>
        Logger.error("EventController.createEventByFacebookId: nothing saved")
        Ok(Json.toJson("EventController.createEventByFacebookId: nothing saved"))
    }
  }
}