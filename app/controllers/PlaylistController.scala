package controllers

import models.Playlist
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import securesocial.core._
import play.api.libs.concurrent.Execution.Implicits._

object PlaylistController extends Controller with securesocial.core.SecureSocial {

  def findByUser = SecuredAction { implicit request =>
    Ok(Json.toJson(Playlist.findByUserId(request.user.identityId.userId)))
  }

  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "tracksId" -> seq(
      mapping(
        "trackId" -> longNumber
      )(Playlist.trackIdFormApply)(Playlist.trackIdFormUnapply))
  )(Playlist.formApply)(Playlist.formUnapply))

  def create = SecuredAction { implicit request =>
    playlistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistNameAndTracksId => {
        val userId = request.user.identityId.userId
        Ok(Json.toJson(Playlist.save(
          Playlist(None, userId, playlistNameAndTracksId.name, playlistNameAndTracksId.tracksId))))
      }
    )
  }
}
