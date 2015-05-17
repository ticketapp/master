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

object PlaylistController extends Controller with securesocial.core.SecureSocial {

  def find(playlistId: Long) = Action { Ok(Json.toJson(Playlist.find(playlistId))) }

  def findByUser = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Playlist.findByUserId(request.user.identityId.userId)))
  }

  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "tracksId" -> seq(mapping(
      "trackId" -> longNumber,
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
        val playlistId = Playlist.save(userId, playlistNameAndTracksId)
        Ok(Json.toJson(playlistId))
      }
    )
  }

  val updatePlaylistBindingForm = Form(mapping(
    "playlistId" -> longNumber,
    "tracksInfo" -> seq(mapping(
      "trackId" -> longNumber,
      "action" -> nonEmptyText,
      "trackRank" -> optional(bigDecimal)
    )(Playlist.trackInfoFormApply)(Playlist.trackInfoFormUnapply))
  )(Playlist.updateFormApply)(Playlist.updateFormUnapply))

  def update() = SecuredAction(ajaxCall = true) { implicit request =>
    updatePlaylistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
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
    Ok(Json.toJson(Playlist.delete(request.user.identityId.userId, playlistId)))
  }
}
