package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

class PlaylistController @Inject() (ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val playlistMethods: PlaylistMethods,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] with playlistFormsTrait {

  def findByUser = SecuredAction.async { implicit request =>
   playlistMethods.findByUserId(request.identity.uuid) map { playlists =>
     Ok(Json.toJson(playlists))
   }
  }

  def create = SecuredAction.async { implicit request =>
    playlistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      playlistNameAndTracksId => {
        val userId = request.identity.uuid
        playlistMethods.saveWithTrackRelations(PlaylistWithTracksIdAndRank(Playlist(None, userId, playlistNameAndTracksId.name),
          playlistNameAndTracksId.tracksIdAndRank)) map { playlist =>
          Ok(Json.toJson(playlist))
        } recover {
          case batchUpdateException: java.sql.BatchUpdateException =>
            Logger.error("PlaylistController.create: ", batchUpdateException.getNextException)
            InternalServerError
          case e: Exception =>
            Logger.error("PlaylistController.create: ", e)
            InternalServerError
        }
      }
    )
  }

  def delete(playlistId: Long) = SecuredAction.async { implicit request =>
    playlistMethods.delete(playlistId) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("PlaylistController.delete: unable to delete the playlist")
        NotModified
    } recover {
      case e: Exception =>
        Logger.error("PlaylistController.delete: ", e)
        InternalServerError
    }
  }

  def update(playlistId: Long) = SecuredAction.async { implicit request =>
    playlistBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("PlaylistController.update: wrong input: " + formWithErrors.errorsAsJson)
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      playlistNameAndTracksId => {
        val userId = request.identity.uuid
        val playlistToUpdate = PlaylistWithTracksIdAndRank(Playlist(Option(playlistId), userId,
          playlistNameAndTracksId.name), playlistNameAndTracksId.tracksIdAndRank)

        playlistMethods.update(playlistToUpdate) map {
          case 0 =>
            Logger.error("PlaylistController.update: no id for this playlist")
            NotModified
          case id =>
            Ok(Json.toJson(id))
        } recover {
          case NonFatal(e) =>
            Logger.error("PlaylistController.update:\nMessage: ", e)
            InternalServerError
        }
      }
    )
  }
}
