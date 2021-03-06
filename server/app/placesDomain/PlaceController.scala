package placesDomain

import javax.inject.Inject

import addresses.{SearchGeographicPoint, AddressFormsTrait}
import application.{Administrator, User}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import database.{EventPlaceRelation, UserPlaceRelation}
import json.JsonHelper._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities

import scala.concurrent.Future
import scala.util.{Failure, Success}


class PlaceController @Inject() (ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val geographicPointMethods: SearchGeographicPoint,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry,
                                 val placeMethods: PlaceMethods)
    extends Silhouette[User, CookieAuthenticator] with AddressFormsTrait with placeFormsTrait with Utilities {

  def places(geographicPoint: String, numberToReturn: Int, offset: Int) = Action.async {
    geographicPointMethods.stringToTryPoint(geographicPoint) match {
      case Failure(exception) =>
        Logger.error("PlaceController.places: invalid geographicPoint")
        Future(BadRequest(Json.toJson("Invalid geographicPoint")))

      case Success(point) =>
        placeMethods.findNear(geographicPoint = point, numberToReturn = numberToReturn, offset = offset) map { places =>
          Ok(Json.toJson(places))
        }
    }
  }

  def createPlace = SecuredAction(Administrator()).async { implicit request =>
    placeBindingForm.bindFromRequest().fold(
      formWithErrors => Future { BadRequest(formWithErrors.errorsAsJson) },
      place => {
        placeMethods.saveWithAddress(place) map { placeCreated =>
          Ok(Json.toJson(placeCreated))
        } recover {
          case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
            Logger.error(s"PlaceController.createPlace: this place already exist")
            Conflict

          case throwable: Throwable =>
            Logger.error("PlaceController.createPlace: INTERNAL_SERVER_ERROR: ", throwable)
            InternalServerError("PlaceController.createPlace: " + throwable.getMessage)
        }
      }
    )
  }

  def findPlacesContaining(pattern: String) = Action.async {
    placeMethods.findAllContaining(pattern) map { places =>
      Ok(Json.toJson(places))
    } recover { case t: Throwable =>
      Logger.error("PlaceController.findPlacesContaining: ", t)
      InternalServerError("PlaceController.findPlacesContaining: " + t.getMessage)
    }
  }

  def findById(id: Long) = Action.async {
    placeMethods.findById(id) map { places =>
      Ok(Json.toJson(places))
    } recover { case t: Throwable =>
      Logger.error("PlaceController.findById: ", t)
      InternalServerError("PlaceController.findById: " + t.getMessage)
    }
  }

  def followByPlaceId(placeId: Long) = SecuredAction.async { implicit request =>
    placeMethods.followByPlaceId(UserPlaceRelation(request.identity.uuid, placeId)) map {
      case 1 =>
        Created

      case _ =>
        Logger.error("PlaceController.followPlace: placeMethods.follow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"PlaceController.followPlaceByPlaceId: $placeId is already followed")
        Conflict

      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"PlaceController.followPlaceByPlaceId: there is no place with the id $placeId")
        NotFound

      case unknownException =>
        Logger.error("PlaceController.followPlace", unknownException)
        InternalServerError
    }
  }

  def unfollowByPlaceId(placeId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    placeMethods.unfollow(UserPlaceRelation(userId, placeId)) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("PlaceController.unfollowPlace: placeMethods.unfollow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the place (placeId: $placeId) or the place does not exist.")
        NotFound
      case unknownException =>
        Logger.error("PlaceController.unfollowPlace", unknownException)
        InternalServerError
    }
  }

  def followByFacebookId(facebookId : String) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    placeMethods.followByFacebookId(userId, facebookId) map {
      case 1 =>
        Created

      case _ =>
        Logger.error("PlaceController.followByFacebookId: placeMethods.followByFacebookId did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the place (placeFacebookId: $facebookId) or the place does not exist.")
        NotFound
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"The user (id: $userId) already follow placeFacebookId: $facebookId).")
        Conflict
      case unknownException =>
        Logger.error("PlaceController.followPlaceByFacebookId", unknownException)
        InternalServerError
    }
  }

  def findFollowed = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    placeMethods.findFollowedPlaces(userId) map { places =>
      Ok(Json.toJson(places))
    } recover { case t: Throwable =>
      Logger.error("PlaceController.getFollowedPlaces: ", t)
      InternalServerError("PlaceController.getFollowedPlaces: " + t.getMessage)
    }
  }

  def isFollowed(placeId: Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    placeMethods.isFollowed(UserPlaceRelation(userId, placeId)) map { places =>
      Ok(Json.toJson(places))
    } recover { case t: Throwable =>
      Logger.error("PlaceController.isPlaceFollowed: ", t)
      InternalServerError("PlaceController.isPlaceFollowed: " + t.getMessage)
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action.async {
    placeMethods.findNearCity(city, numberToReturn, offset) map { places =>
      Ok(Json.toJson(places))
    }
  }

  def deleteEventRelation(eventId: Long, placeId: Long) = SecuredAction(Administrator()).async {
    placeMethods.deleteEventRelation(EventPlaceRelation(eventId, placeId)) map { result =>
      Ok(Json.toJson(result))
    }
  }

  def saveEventRelation(eventId: Long, placeId: Long) = SecuredAction(Administrator()).async {
    placeMethods.saveEventRelation(EventPlaceRelation(eventId, placeId)) map { result =>
      Ok(Json.toJson(result))
    }
  }
}
