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
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class EventController @Inject()(ws: WSClient,
                                val messagesApi: MessagesApi,
                                val utilities: Utilities,
                                val geographicPointMethods: SearchGeographicPoint,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry,
                                val eventMethods: EventMethods)
  extends Silhouette[User, CookieAuthenticator] {

  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def events(offset: Int, numberToReturn: Int, geographicPoint: String) = Action.async {
    geographicPointMethods.stringToGeographicPoint(geographicPoint) match {
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
    geographicPointMethods.stringToGeographicPoint(geographicPoint) match {
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
    geographicPointMethods.stringToGeographicPoint(geographicPoint) match {
      case Success(point) =>
        eventMethods.findPassedInHourIntervalNear(hourInterval, point, offset, numberToReturn) map { events =>
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
    geographicPointMethods.stringToGeographicPoint(geographicPointString) match {
      case Success(point) =>
        eventMethods.findAllByGenre(genre, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("EventController.findByGenre: Invalid geographic point"))
    }
  }

  def findAllContaining(pattern: String, center: String) = Action.async {
    geographicPointMethods.stringToGeographicPoint(center) match {
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

  val eventBindingForm = Form(
    mapping(
      "facebookId"-> optional(nonEmptyText(3)),
      "name" -> nonEmptyText(2),
      "geographicPoint" -> optional(nonEmptyText(3)),
      "description" -> optional(nonEmptyText(2)),
      "startTime" -> jodaDate("yyyy-MM-dd HH:mm"),
      "endTime" -> optional(jodaDate("yyyy-MM-dd HH:mm")),
      "ageRestriction" -> number,
      "imagePath" -> optional(nonEmptyText(2)),
      "tariffRange" -> optional(nonEmptyText(3)),
      "ticketSellers" -> optional(nonEmptyText(3))//,
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
    )(eventMethods.formApply)(eventMethods.formUnapply)
  )

  def createEvent = Action.async { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      event =>
        eventMethods.save(event) map { event =>
          Ok(Json.toJson(event))
        } recover {
          case e =>
            Logger.error("EventController.createEvent: ", e)
            InternalServerError
        }
    )
  }

  def followEvent(eventId : Long) = SecuredAction.async { implicit request =>
    eventMethods.follow(UserEventRelation(request.identity.uuid, eventId)) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("EventController.followEvent: eventMethods.follow did not return 1")
        InternalServerError
     } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(s"EventController.followEventByEventId: $eventId is already followed")
        Conflict
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(s"EventController.followEventByEventId: there is no event with the id $eventId")
        NotFound
      case unknownException =>
        Logger.error("EventController.followEvent", unknownException)
        InternalServerError
    }
  }

  def unfollowEvent(eventId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    eventMethods.unfollow(UserEventRelation(userId, eventId)) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("EventController.unfollowEvent: eventMethods.unfollow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the event (eventId: $eventId) or the event does not exist.")
        Conflict
      case unknownException =>
        Logger.error("EventController.unfollowEvent", unknownException)
        InternalServerError
    }
  }

  def isEventFollowed(eventId: Long) = SecuredAction.async { implicit request =>
    eventMethods.isFollowed(UserEventRelation(request.identity.uuid, eventId)) map { boolean =>
      Ok(Json.toJson(boolean))
    } recover {
      case e =>
        Logger.error("EventController.isEventFollowed: ", e)
        InternalServerError
    }
  }

  def getFollowedEvents = SecuredAction.async { implicit request =>
    eventMethods.getFollowedEvents(request.identity.uuid) map { events =>
      Ok(Json.toJson(events))
    } recover {
      case e =>
        Logger.error("EventController.getFollowedEvents: ", e)
        InternalServerError
    }
  }

  def createEventByFacebookId(facebookId: String) = Action.async {
    eventMethods.saveFacebookEventByFacebookId(facebookId) map { event =>
      Ok(Json.toJson(event))
    }
  }
}