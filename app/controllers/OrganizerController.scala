package controllers

import models.Organizer
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
import json.JsonHelper.organizerWrites
import securesocial.core.Identity

import scala.util.{Failure, Success}
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}

object OrganizerController extends Controller with securesocial.core.SecureSocial {
  def organizers(numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Organizer.findAll(numberToReturn: Int, offset: Int)))
  }

  def organizer(id: Long) = Action {
    Ok(Json.toJson(Organizer.find(id)))
  }

  def findOrganizersContaining(pattern: String) = Action {
    Ok(Json.toJson(Organizer.findAllContaining(pattern)))
  }

  def followOrganizerByOrganizerId(organizerId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Organizer.followByOrganizerId(request.user.identityId.userId, organizerId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
        Status(CONFLICT)("This user already follow this organizer.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
        Status(CONFLICT)("There is no organizer with this id.")
      case Failure(unknownException) =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }

  def followOrganizerByFacebookId(facebookId : String) = SecuredAction(ajaxCall = true) { implicit request =>
    Organizer.followByFacebookId(request.user.identityId.userId, facebookId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByFacebookId", psqlException)
        Status(CONFLICT)("This user already follow this organizer.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByFacebookId", psqlException)
        Status(CONFLICT)("There is no organizer with this id.")
      case Failure(unknownException) =>
        Logger.error("OrganizerController.followOrganizerByFacebookId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
  }

  def getFollowedOrganizers = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Organizer.getFollowedOrganizers(identity.identityId)))
    }
  }

  def isOrganizerFollowed(organizerId: Long) = UserAwareAction { implicit request =>
    request.user match {
      case None => Ok(Json.toJson("User not connected"))
      case Some(identity: Identity) => Ok(Json.toJson(Organizer.isFollowed(identity.identityId, organizerId)))
    }
  }

  val organizerBindingForm = Form(
    mapping(
      "facebookId" -> optional(nonEmptyText(2)),
      "name" -> nonEmptyText(2),
      "imagePath" -> optional(nonEmptyText(2))
    )(Organizer.formApply)(Organizer.formUnapply)
  )

  def createOrganizer = Action { implicit request =>
    organizerBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      organizer => {
        Organizer.save(organizer) match {
          case Success(maybeOrganizerId) => maybeOrganizerId match {
            case None => Status(INTERNAL_SERVER_ERROR)
            case Some(organizerId) => Ok(Json.toJson(Organizer.find(organizerId)))
          }
          case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
            Logger.error("OrganizerController.createOrganizer: Duplicate organizer", psqlException)
            Status(CONFLICT)("OrganizerController.createOrganizer: Duplicate organizer")
          case Failure(unknownException) =>
            Logger.error("OrganizerController.createOrganizer: INTERNAL_SERVER_ERROR", unknownException)
            Status(INTERNAL_SERVER_ERROR)
        }
      }
    )
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Organizer.findNearCity(city, numberToReturn, offset)))
  }
}

