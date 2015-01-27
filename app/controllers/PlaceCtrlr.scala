package controllers

import models.Place
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json


object PlaceController extends Controller {
  def places = Action {
    Ok(Json.toJson(Place.findAll()))
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
}
