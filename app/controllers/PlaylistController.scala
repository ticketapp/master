package controllers

import models.Playlist
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

object PlaylistController extends Controller {

  def findByUserId(userId: Long) = Action {
    Ok(Json.toJson(Playlist.findByUserId(userId)))
  }
}
