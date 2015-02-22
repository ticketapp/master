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

object PlaceController extends Controller {
  def places = Action {
    Ok(Json.toJson(Place.findAll))
  }

  def place(id: Long) = Action {
    Ok(Json.toJson(Place.find(id)))
  }

  def findPlacesContaining(pattern: String) = Action {
    Ok(Json.toJson(Place.findAllContaining(pattern)))
  }

  def deletePlace(placeId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM places WHERE placeId={placeId}").on(
        'placeId -> placeId
      ).executeUpdate()
    }
  }

  def followPlace(userId : Long, placeId : Long) = Action {
    Place.followPlace(userId, placeId)
    Redirect(routes.Admin.indexAdmin())
  }

  val placeBindingForm = Form(mapping(
    "name" -> nonEmptyText(2),
    "facebookId" -> optional(nonEmptyText()),
    "description" -> optional(nonEmptyText(2)),
    "webSite" -> optional(nonEmptyText(4)),
    "capacity" -> optional(number),
    "openingHours" -> optional(nonEmptyText(4))
  )(Place.formApply)(Place.formUnapply)
  )
  
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
