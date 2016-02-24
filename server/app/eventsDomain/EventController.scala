package eventsDomain

import javax.inject.Inject

import addresses.SearchGeographicPoint
import application.{Administrator, User}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import database.UserEventRelation
import json.JsonHelper
import json.JsonHelper._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.LoggerHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


class EventController @Inject()(ws: WSClient,
                                val messagesApi: MessagesApi,
                                val geographicPointMethods: SearchGeographicPoint,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry,
                                val eventMethods: EventMethods)
    extends Silhouette[User, CookieAuthenticator] with EventFormsTrait with LoggerHelper {

  def events(offset: Int, numberToReturn: Int, geographicPoint: String) = Action.async {
    geographicPointMethods.stringToTryPoint(geographicPoint) match {
      case Failure(exception) =>
        Logger.error("EventController.events: ", exception)
        Future(BadRequest(Json.toJson("Invalid geographicPoint")))
      case Success(point) =>
        eventMethods.findNear(point, numberToReturn: Int, offset: Int) map { events =>
          Ok(Json.toJson(events))
        } recover {
          case t: Throwable =>
            Logger.error("EventController.events: ", t)
            InternalServerError("EventController.events: " + t.getMessage)
        }
    }
  }

  def eventsInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int) = Action.async {
    geographicPointMethods.stringToTryPoint(geographicPoint) match {
      case Success(point) =>
        eventMethods.findInPeriodNear(hourInterval, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Logger.error("EventController.eventsInHourInterval: invalid geographicPoint")
        Future(BadRequest("EventController.eventsInHourInterval: invalid geographicPoint"))
    }
  }

  def eventsPassedInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int)
  = Action.async {
    geographicPointMethods.stringToTryPoint(geographicPoint) match {
      case Success(point) =>
        eventMethods.findPassedInHourIntervalNear(hourInterval, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("EventController.eventsPassedInHourInterval: invalid geographic point"))
    }
  }

  def find(id: Long) = Action.async {
    eventMethods.find(id) map {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }

  def update() = SecuredAction(Administrator()).async { request =>
    request.body.asJson match {
      case Some(event) =>
        event.validate[Event] match {

          case successEvent: JsSuccess[Event] =>
            eventMethods.update(successEvent.get) map { response =>
              Ok(Json.toJson(response))
            }

          case error: JsError =>
            log(error.toString)
            Future(BadRequest("Bad event object:" + error))
        }

      case _ =>
        log("Bad event object")
        Future(BadRequest("Bad event object"))
    }
  }

  def findByPlace(placeId: Long) = Action.async {
    eventMethods.findAllNotFinishedByPlace(placeId) map { events =>
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
    geographicPointMethods.stringToTryPoint(geographicPointString) match {
      case Success(point) =>
        eventMethods.findAllByGenre(genre, point, offset, numberToReturn) map { events =>
          Ok(Json.toJson(events))
        }
      case _ =>
        Future(BadRequest("EventController.findByGenre: Invalid geographic point"))
    }
  }

  def findAllContaining(pattern: String, center: String) = Action.async {
    geographicPointMethods.stringToTryPoint(center) match {
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

  def createEvent = SecuredAction(Administrator()).async { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      event =>
        eventMethods.save(EventWithRelations(event)) map { event =>
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
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"EventController.followEventByEventId: $eventId is already followed")
        Conflict
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
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
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
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
    eventMethods.findFollowedEvents(request.identity.uuid) map { events =>
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