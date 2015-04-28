package controllers

import models.Place
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
import json.JsonHelper.placeWrites
import securesocial.core.Identity

import scala.util.matching.Regex

object PlaceController extends Controller with securesocial.core.SecureSocial {
  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def places(geographicPoint: String, numberToReturn: Int, offset: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) => Ok(Json.toJson(Place.findNear(geographicPoint, numberToReturn, offset)))
      case _ => Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def place(id: Long) = Action {
    Ok(Json.toJson(Place.find(id)))
  }

  def findPlacesContaining(pattern: String) = Action {
    Ok(Json.toJson(Place.findAllContaining(pattern)))
  }

  def findPlacesNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Place.findNearCity(city, numberToReturn, offset)))
  }

  def deletePlace(placeId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM places WHERE placeId={placeId}").on(
        'placeId -> placeId
      ).executeUpdate()
    }
  }

  def followPlaceByPlaceId(artistId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Place.followPlaceByPlaceId(request.user.identityId.userId, artistId)))
  }

  def followPlaceByFacebookId(facebookId : String) = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Place.followPlaceByFacebookId(request.user.identityId.userId, facebookId)))
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
  
  def createPlace = Action { implicit request =>
    placeBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      place => {
        Place.save(place) match {
          case Some(eventId) => Ok(Json.toJson(Place.find(eventId)))
          case None => Ok(Json.toJson("The place couldn't be saved"))
        }
      }
    )
  }
}
