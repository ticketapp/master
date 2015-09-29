package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper.placeWrites
import models.{PlaceMethods, Place, User}
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities
//import services.Utilities.{FOREIGN_KEY_VIOLATION, UNIQUE_VIOLATION, geographicPointPattern}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PlaceController @Inject() (ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val utilities: Utilities,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry,
                                  val placeMethods: PlaceMethods)
  extends Silhouette[User, CookieAuthenticator] {
/*
  def places(geographicPoint: String, numberToReturn: Int, offset: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) => Ok(Json.toJson(placeMethods.findNear(geographicPoint, numberToReturn, offset)))
      case _ => Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def place(id: Long) = Action { Ok(Json.toJson(placeMethods.find(id))) }

  def findPlacesContaining(pattern: String) = Action { Ok(Json.toJson(placeMethods.findAllContaining(pattern))) }

  def findPlacesNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(placeMethods.findNearCity(city, numberToReturn, offset)))
  }

  def followPlaceByPlaceId(placeId : Long) = SecuredAction { implicit request =>
    val userId = request.identity.UUID
    placeMethods.followByPlaceId(userId, placeId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"PlaceController.followPlaceByPlaceId: user with id $userId already follows place with id $placeId")
        Conflict
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"PlaceController.followPlaceByPlaceId: there is no place with the id $placeId")
        NotFound
      case Failure(unknownException) =>
        Logger.error("PlaceController.followPlaceByPlaceId", unknownException)
        InternalServerError
    }
  }

  def unfollowPlaceByPlaceId(placeId : Long) = SecuredAction { implicit request =>
    val userId = request.identity.UUID
    placeMethods.unfollowByPlaceId(userId, placeId) match {
      case Success(1) =>
        Ok
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the place (placeId: $placeId) or the place does not exist.")
        NotFound
      case Failure(unknownException) =>
        Logger.error("PlaceController.followPlaceByPlaceId", unknownException)
        InternalServerError
    }
  }

  def followPlaceByFacebookId(facebookId : String) = SecuredAction { implicit request =>
    val userId = request.identity.UUID
    placeMethods.followByFacebookId(userId, facebookId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(
          s"""PlaceController.followPlaceByFacebookId: user with id $userId already follows
             |place with facebook id $facebookId""".stripMargin)
        Conflict("This user already follows this place.")
      case Failure(thereIsNoPlaceForThisFacebookIdException: ThereIsNoPlaceForThisFacebookIdException) =>
        Logger.error(s"PlaceController.followPlaceByFacebookId : there is no place with the facebook id $facebookId")
        NotFound
      case Failure(unknownException) =>
        Logger.error("PlaceController.followPlaceByFacebookId", unknownException)
        InternalServerError
    }
  }
  
  def isPlaceFollowed(placeId: Long) = UserAwareAction { implicit request =>
    request.identity match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(user: User) => Ok(Json.toJson(placeMethods.isFollowed(user.UUID, placeId)))
    }
  }

  def getFollowedPlaces = SecuredAction { implicit request =>
    Ok(Json.toJson(placeMethods.getFollowedPlaces(request.identity.UUID)))
  }

  val placeBindingForm = Form(mapping(
    "name" -> nonEmptyText(2),
    "facebookId" -> optional(nonEmptyText()),
    "geographicPoint" -> optional(nonEmptyText(5)),
    "description" -> optional(nonEmptyText(2)),
    "webSite" -> optional(nonEmptyText(4)),
    "capacity" -> optional(number),
    "openingHours" -> optional(nonEmptyText(4)),
    "imagePath" -> optional(nonEmptyText(2)),
    "city" -> optional(nonEmptyText(2)),
    "zip" -> optional(nonEmptyText(3)),
    "street" -> optional(nonEmptyText(2))
  )(placeMethods.formApply)(placeMethods.formUnapply))
  
  def create = Action.async { implicit request =>
    placeBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString(), new Exception("PlaceController.create"))
        Future {
          BadRequest(formWithErrors.errorsAsJson)
        }
      },
      place => {
        placeMethods.save(place) map {
          case Success(Some(placeId)) =>
            Ok(Json.toJson(placeMethods.find(placeId)))
          case Success(None) =>
            Logger.error("PlaceController.createPlace")
            Status(INTERNAL_SERVER_ERROR)
          case Failure(exception) =>
            Logger.error("PlaceController.createPlace", exception)
            Status(INTERNAL_SERVER_ERROR)
        }
      }
    )
  }*/
}
