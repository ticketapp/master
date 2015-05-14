package controllers

import models.Place
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
import json.JsonHelper.placeWrites
import securesocial.core.Identity
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.matching.Regex
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION, geographicPointPattern}

object PlaceController extends Controller with securesocial.core.SecureSocial {

  def places(geographicPoint: String, numberToReturn: Int, offset: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) => Ok(Json.toJson(Place.findNear(geographicPoint, numberToReturn, offset)))
      case _ => Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def place(id: Long) = Action { Ok(Json.toJson(Place.find(id))) }

  def findPlacesContaining(pattern: String) = Action { Ok(Json.toJson(Place.findAllContaining(pattern))) }

  def findPlacesNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Place.findNearCity(city, numberToReturn, offset)))
  }

  def followPlaceByPlaceId(placeId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Place.followByPlaceId(request.user.identityId.userId, placeId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error("PlaceController.followPlaceByPlaceId", psqlException)
        Status(CONFLICT)("This user already follow this place.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error("PlaceController.followPlaceByPlaceId", psqlException)
        Status(CONFLICT)("There is no place with this id.")
      case Failure(unknownException) =>
        Logger.error("PlaceController.followPlaceByPlaceId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }

  def followPlaceByFacebookId(facebookId : String) = SecuredAction(ajaxCall = true) { implicit request =>
    Place.followByFacebookId(request.user.identityId.userId, facebookId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error("PlaceController.followPlaceByFacebookId", psqlException)
        Status(CONFLICT)("This user already follow this place.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error("PlaceController.followPlaceByFacebookId", psqlException)
        Status(CONFLICT)("There is no place with this id.")
      case Failure(unknownException) =>
        Logger.error("PlaceController.followPlaceByFacebookId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }
  
  def isPlaceFollowed(placeId: Long) = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Place.isFollowed(identity.identityId, placeId)))
    }
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
    "street" -> optional(nonEmptyText(2)),
    "city" -> optional(nonEmptyText(2)),
    "zip" -> optional(nonEmptyText(3))
  )(Place.formApply)(Place.formUnapply))
  
  def createPlace = Action.async { implicit request =>
    placeBindingForm.bindFromRequest().fold(
      formWithErrors => Future { BadRequest(formWithErrors.errorsAsJson) },
      place => {
        Place.save(place) map {
          case Success(Some(placeId)) =>
            Ok(Json.toJson(Place.find(placeId)))
          case Success(None) =>
            Logger.error("PlaceController.createPlace")
            Status(INTERNAL_SERVER_ERROR)
          case Failure(psqlException) =>
            Logger.error("PlaceController.createPlace")//, psqlException)
            Status(INTERNAL_SERVER_ERROR)
        }
      }
    )
  }
}
