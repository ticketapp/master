package controllers

import models.Track
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.util.{Failure, Success}

object TrackController extends Controller with securesocial.core.SecureSocial {
  val trackBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "url" -> nonEmptyText(3),
    "platform" -> nonEmptyText,
    "thumbnailUrl" -> nonEmptyText(2),
    "artistFacebookUrl" -> nonEmptyText(2),
    "redirectUrl" -> optional(nonEmptyText(2))
  )(Track.formApply)(Track.formUnapply))

  def createTrack = Action { implicit request =>
    trackBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      track => {
        Track.save(track)
        match {
          case Success(Some(trackId)) => Ok(Json.toJson(Track.find(trackId)))
          case _ => InternalServerError
        }
      }
    )
  }

  def upsertRating(trackId: Long, rating: Int) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Track.upsertRating(userId, trackId, rating) match {
      case Success(true) => Ok
      case _ => InternalServerError
    }
  }

  def getRating(trackId: Long) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Track.getRating(userId, trackId) match {
      case Success(Some(rating)) => Ok(Json.toJson(rating))
      case _ => InternalServerError
    }
  }

  def addToFavorites(trackId: Long) = SecuredAction(ajaxCall = true) { implicit request =>
    val userId = request.user.identityId.userId
    Track.addToFavorites(userId, trackId) match {
      case Success(1) =>
        Ok
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Conflict(s"This user already has track $trackId in his favorites.")
      case _ =>
        InternalServerError
    }
  }
}