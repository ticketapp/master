package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TrackController @Inject() (ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val trackMethods: TrackMethods,
                                 val trackRatingMethods: TrackRatingMethods,
                                 val utilities: Utilities,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] with trackFormsTrait {

  val FOREIGN_KEY_VIOLATION = utilities.FOREIGN_KEY_VIOLATION
  val UNIQUE_VIOLATION = utilities.UNIQUE_VIOLATION

  def createTrack = Action.async { implicit request =>
    trackBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("TrackController.createTrack: " + formWithErrors.errorsAsJson)
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      track =>
        trackMethods.save(track) map { track =>
          Ok(Json.toJson(track))
        } recover {
          case e =>
            Logger.error("TrackController.createTrack: ", e)
            InternalServerError
        }
    )
  }

  def findAllByArtist(artistFacebookUrl: String, numberToReturn: Int, offset: Int) = Action.async {
    trackMethods.findAllByArtist(artistFacebookUrl, numberToReturn, offset) map { tracks =>
      Ok(Json.toJson(tracks))
    } recover { case t: Throwable =>
      Logger.error("TrackController.findAllByArtist: ", t)
      InternalServerError("TrackController.findAllByArtist: " + t.getMessage)
    }
  }

  def upsertRatingForUser = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    trackRatingBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      trackRating => {
        val trackId = UUID.fromString(trackRating.trackId)
        trackRating.rating match {
          case rating if rating != 0 =>
            trackRatingMethods.upsertRatingForAUser(userId, trackId, rating) map {
              case 1 =>
                Ok
              case _ =>
                Logger.error(s"TrackController.upsertRatingForUser: 1 was not returned by " +
                  s"trackRatingMethods.upsertRatingForAUser for user id $userId and track.uuid $trackId")
                InternalServerError
            }
          case _ =>
            Logger.error("TrackController.upsertRatingForUser: nothing has been changed since the ratting to add is 0")
            Future(BadRequest)
        }
      }
    )
  }

//  def getRatingForUser(trackId: String) = SecuredAction.async { implicit request =>
//    val userId = request.identity.uuid
//    trackRatingMethods.getRatingForUser(userId, UUID.fromString(trackId)) map {
//      case Some(rating) =>
//        Ok(Json.toJson(rating.ratingUp.toString + "," + rating.ratingDown.toString))
//      case None =>
//        Logger.error(s"TrackController.getRatingForUser: there is no rating for the track.uuid $trackId and user id $userId")
//        NotFound
//    } recover {
//      case t: Throwable =>
//        Logger.error("TrackController.getRatingForUser:", t)
//        InternalServerError
//    }
//  }
  
  def followTrack(trackId: String) = SecuredAction.async { implicit request =>
    Try(UUID.fromString(trackId)) match {
      case Success(trackUUID) =>
        trackMethods.followByTrackId(UserTrackRelation(userId = request.identity.uuid, trackId = trackUUID)) map {
          case 1 =>
            Created
          case _ =>
            Logger.error("TrackController.followTrack: trackMethods.follow did not return 1")
            InternalServerError
        } recover {
          case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
            Logger.error(s"TrackController.followTrackByTrackId: $trackId is already followed")
            Conflict
          case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
            Logger.error(s"TrackController.followTrackByTrackId: there is no track with the id $trackId")
            NotFound
          case unknownException =>
            Logger.error("TrackController.followTrack", unknownException)
            InternalServerError
        }
      case Failure(t: Throwable) =>
        Logger.error("TrackController.followTrack: trackMethods.follow did not return 1")
        Future(InternalServerError("track uuid is not valid"))
    }
  }

  def unfollowTrack(trackId: String) = SecuredAction.async { implicit request =>
    Try(UUID.fromString(trackId)) match {
      case Success(trackUUID) =>
        val userId = request.identity.uuid
        trackMethods.unfollowByTrackId(UserTrackRelation(userId = request.identity.uuid, trackId = trackUUID)) map {
          case 1 =>
            Ok
          case _ =>
            Logger.error("TrackController.unfollowTrack: trackMethods.unfollow did not return 1")
            InternalServerError
        } recover {
          case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
            Logger.error(s"The user (id: $userId) does not follow the track (trackId: $trackId).")
            NotFound
          case unknownException =>
            Logger.error("TrackController.unfollowTrack: unknownException: ", unknownException)
            InternalServerError
        }
      case Failure(t: Throwable) =>
        Logger.error("TrackController.followTrack: trackMethods.follow did not return 1")
        Future(InternalServerError("track uuid is not valid"))
    }
  }

  def getFollowedTracks = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    trackMethods.getFollowedTracks(userId) map { tracks =>
      Ok(Json.toJson(tracks))
    } recover { case t: Throwable =>
      Logger.error("TrackController.getFollowedTracks: ", t)
      InternalServerError("TrackController.getFollowedTracks: " + t.getMessage)
    }
  }

  def isTrackFollowed(trackId: String) = SecuredAction.async { implicit request =>
    Try(UUID.fromString(trackId)) match {
      case Success(trackUUID) =>
        val userId = request.identity.uuid
        trackMethods.isFollowed(UserTrackRelation(userId, trackUUID)) map { tracks =>
          Ok(Json.toJson(tracks))
        } recover { case t: Throwable =>
          Logger.error("TrackController.isTrackFollowed: ", t)
          InternalServerError("TrackController.isTrackFollowed: " + t.getMessage)
        }
      case Failure(t: Throwable) =>
        Logger.error("TrackController.followTrack: trackMethods.follow did not return 1")
        Future(InternalServerError("track uuid is not valid"))
    }
  }
}