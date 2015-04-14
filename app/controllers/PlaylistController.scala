package controllers

import models.Playlist
import models.Playlist.existsPlaylistForUser
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import securesocial.core._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

object PlaylistController extends Controller with securesocial.core.SecureSocial {

  def findByUser = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Playlist.findByUserId(request.user.identityId.userId)))
  }

  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "tracksId" -> seq(mapping(
      "trackId" -> longNumber
    )(Playlist.trackIdFormApply)(Playlist.trackIdFormUnapply))
  )(Playlist.formApply)(Playlist.formUnapply))

  def create = SecuredAction(ajaxCall = true) { implicit request =>
    playlistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistNameAndTracksId => {
        val userId = request.user.identityId.userId
        val playlistId = Playlist.save(userId, playlistNameAndTracksId)
        Ok(Json.toJson(playlistId))
      }
    )
  }

  val addTracksInPlaylistBindingForm = Form(mapping(
    "id" -> longNumber,
    "tracksId" -> seq(mapping(
      "trackId" -> longNumber
    )(Playlist.trackIdFormApply)(Playlist.trackIdFormUnapply))
  )(Playlist.addOrRemoveTracksFormApply)(Playlist.addOrRemoveTracksFormUnapply))


  def addTracks() = SecuredAction(ajaxCall = true) { implicit request =>
    addTracksInPlaylistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistIdAndTracksId => {
        val userId = request.user.identityId.userId
        //if(existsPlaylistForUser(userId, playlistIdAndTracksId.id)) {
          Future { Playlist.addTracksInPlaylist(userId, playlistIdAndTracksId) }
          Ok
        /*} else
          Ok(Json.toJson("There is no playlist with this playlist Id for this user"))*/
      }
    )
  }

  def deleteTracks() = SecuredAction(ajaxCall = true) { implicit request =>
    addTracksInPlaylistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistIdAndTracksId => {
        val userId = request.user.identityId.userId
        //if(existsPlaylistForUser(userId, playlistIdAndTracksId.id)) {
          Future { Playlist.deleteTracksInPlaylist(userId, playlistIdAndTracksId) }
          Ok
        /*} else
          Ok(Json.toJson("There is no playlist with this playlist Id for this user"))*/
      }
    )
  }

  def delete(playlistId: Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Playlist.delete(request.user.identityId.userId, playlistId)))
  }
}
