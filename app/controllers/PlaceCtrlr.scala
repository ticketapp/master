package controllers

import models.{Artist, Place}
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json

import scala.util.{Failure, Success}


object PlaceController extends Controller {
  def places = Action {
    Ok(Json.toJson(Place.findAll))
  }

  def place(id: Long) = Action {
    Ok(Json.toJson(Place.find(id)))
  }

  def placesStartingWith(pattern: String) = Action {
    Ok(Json.toJson(Place.findAllStartingWith(pattern)))
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
    "addressId" -> optional(longNumber),
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
        val placeId = Place.save(place)
        //Redirect(routes.Admin.indexAdmin())
        Ok(Json.toJson(Place.find(placeId)))
      }
    )
  }

}
