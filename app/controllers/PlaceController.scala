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

import scala.util.matching.Regex

object PlaceController extends Controller {
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

  def followPlace(placeId : Long) = Action {
    Place.followPlace(placeId)
    Redirect(routes.Admin.indexAdmin())
  }

  val placeBindingForm = Form(mapping(
    "name" -> nonEmptyText(2),
    "facebookId" -> optional(nonEmptyText()),
    "geographicPoint" -> optional(nonEmptyText(5)),
    "description" -> optional(nonEmptyText(2)),
    "webSite" -> optional(nonEmptyText(4)),
    "capacity" -> optional(number),
    "openingHours" -> optional(nonEmptyText(4)),
    "imagePath" -> optional(nonEmptyText(2))
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
