package controllers

import java.util.UUID

import models.{TrackMethods, Track, User}
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities
import scala.concurrent.Future
import scala.util.{Failure, Success}
import json.JsonHelper._
import play.api.libs.json.DefaultWrites
import javax.inject.Inject
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.MessagesApi
import scala.concurrent.ExecutionContext.Implicits.global

class TrackController @Inject() (ws: WSClient,
                                 val messagesApi: MessagesApi,
                                 val trackMethods: TrackMethods,
                                 val utilities: Utilities,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] {

  val FOREIGN_KEY_VIOLATION = utilities.FOREIGN_KEY_VIOLATION
  val UNIQUE_VIOLATION = utilities.UNIQUE_VIOLATION

  val trackBindingForm = Form(mapping(
    "trackId" -> nonEmptyText(8),
    "title" -> nonEmptyText(2),
    "url" -> nonEmptyText(3),
    "platform" -> nonEmptyText,
    "thumbnailUrl" -> nonEmptyText(2),
    "artistFacebookUrl" -> nonEmptyText(2),
    "artistName" -> nonEmptyText(2),
    "redirectUrl" -> optional(nonEmptyText(2))
  )(trackMethods.formApply)(trackMethods.formUnapply))

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
      Logger.error("TrackController.findOrganizersContaining: ", t)
      InternalServerError("TrackController.findOrganizersContaining: " + t.getMessage)
    }
  }

  val trackRatingBindingForm = Form(mapping(
    "trackId" -> nonEmptyText(8),
    "rating" -> number,
    "reason" -> optional(nonEmptyText)
  )(trackRatingFormApply)(trackRatingFormUnapply))

  case class TrackRating(trackId: String, rating: Int, reason: Option[Char])

  def trackRatingFormApply(trackId: String, rating: Int, reason: Option[String]): TrackRating =
    new TrackRating(trackId, rating, reason match { case None => None; case Some(string) => Option(string(0)) } )
  def trackRatingFormUnapply(trackRating: TrackRating) =
    Some((trackRating.trackId, trackRating.rating,
      trackRating.reason match { case None => None; case Some(char) => Option(char.toString) }))

//  def upsertRatingForUser = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    trackRatingBindingForm.bindFromRequest().fold(
//      formWithErrors => {
//        Logger.error(formWithErrors.errorsAsJson.toString())
//        BadRequest(formWithErrors.errorsAsJson)
//      },
//      trackRating => {
//        val trackIdUUID = UUID.fromString(trackRating.trackId)
//        trackRating.rating match {
//          case ratingUp if ratingUp > 0 =>
//            trackMethods.upsertRatingUp(userId, trackIdUUID, ratingUp) match {
//              case Success(true) => Ok
//              case _ => InternalServerError
//            }
//          case ratingDown if ratingDown < 0 =>
//            trackMethods.upsertRatingDown(userId, trackIdUUID, ratingDown, trackRating.reason) match {
//              case Success(true) => Ok
//              case _ => InternalServerError
//            }
//          case _ => BadRequest
//        }
//      }
//    )
//  }
//
//  def getRatingForUser(trackId: String) = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    Track.getRatingForUser(userId, UUID.fromString(trackId)) match {
//      case Success(Some(rating)) => Ok(Json.toJson(rating._1.toString + "," + rating._2.toString))
//      case _ => InternalServerError
//    }
//  }
//
//  def addToFavorites(trackId: String) = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    trackMethods.addToFavorites(userId, UUID.fromString(trackId)) match {
//      case Success(1) =>
//        Ok
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//        Conflict(s"This user already has track $trackId in his favorites.")
//      case Failure(e: Exception) =>
//        Logger.error("TrackController.addToFavorites", e)
//        InternalServerError
//      case _ =>
//        InternalServerError(s"Track.controller.addTOFavorite: trackId $trackId was not added")
//    }
//  }
//
//  def removeFromFavorites(trackId: String) = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    trackMethods.removeFromFavorites(userId, UUID.fromString(trackId)) match {
//      case Success(1) =>
//        Ok
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//        NotFound(s"The track $trackId is not in the favorites of this user or this track does not exist.")
//      case _ =>
//        InternalServerError
//    }
//  }
//
//  def findFavorites = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    trackMethods.findFavorites(userId) match {
//      case Success(tracks) =>
//        Ok(Json.toJson(tracks))
//      case _ =>
//        InternalServerError
//    }
//  }
}