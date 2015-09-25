package controllers

import models.Playlist
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import json.JsonHelper._

import scala.util.{Failure, Success}

object PlaylistController extends Controller {

  def find(playlistId: Long) = Action { Ok(Json.toJson(Playlist.find(playlistId))) }

  def findByUser = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Playlist.findByUserId(request.user.identityId.userId)))
  }

  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "tracksId" -> seq(mapping(
      "trackId" -> nonEmptyText(6),
      "trackRank" -> bigDecimal
    )(Playlist.idAndRankFormApply)(Playlist.idAndRankFormUnapply))
  )(Playlist.formApply)(Playlist.formUnapply))

  def create = SecuredAction(ajaxCall = true) { implicit request =>
    playlistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistNameAndTracksId => {
        val userId = request.user.identityId.userId
        val playlistId = Playlist.saveWithTrackRelation(userId, playlistNameAndTracksId)
        Ok(Json.toJson(playlistId))
      }
    )
  }

  val updatePlaylistBindingForm = Form(mapping(
    "playlistId" -> longNumber,
    "tracksInfo" -> seq(mapping(
      "trackId" -> nonEmptyText,
      "action" -> nonEmptyText,
      "trackRank" -> optional(bigDecimal)
    )(Playlist.trackInfoFormApply)(Playlist.trackInfoFormUnapply))
  )(Playlist.updateFormApply)(Playlist.updateFormUnapply))

  def update() = SecuredAction(ajaxCall = true) { implicit request =>
    updatePlaylistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("PlaylistController.update:" + formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      playlistIdAndTracksInfo => {
        val userId = request.user.identityId.userId
        Future { Playlist.update(userId, playlistIdAndTracksInfo) }
        Ok
      }
    )
  }

  def delete(playlistId: Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Playlist.delete(request.user.identityId.userId, playlistId) match {
      case Success(1) =>
        Ok
      case Failure(exception) =>
        Logger.error("PlaylistController.delete: ", exception)
        InternalServerError("PlaylistController.delete: unable to delete the playlist" + exception.getMessage)
      case Success(_) =>
        Logger.error("PlaylistController.delete: unable to delete the playlist")
        InternalServerError("PlaylistController.delete: unable to delete the playlist")
    }
  }
}
