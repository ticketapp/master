package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper.organizerWrites
import models.{Organizer, User}
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
//import services.Utilities.{FOREIGN_KEY_VIOLATION, UNIQUE_VIOLATION}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import models.Organizer
import models.OrganizerMethods
import json.JsonHelper._
import json.JsonHelper._

class OrganizerController @Inject()(ws: WSClient,
                                    val organizerMethods: OrganizerMethods,
                                    val messagesApi: MessagesApi,
                                    val env: Environment[User, CookieAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def find(numberToReturn: Int, offset: Int) = Action.async {
    organizerMethods.find(numberToReturn: Int, offset: Int) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.findOrganizersContaining: ", t)
      InternalServerError("OrganizerController.findOrganizersContaining: " + t.getMessage)
    }
  }

  def findById(id: Long) = Action.async {
    organizerMethods.findById(id) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.findById: ", t)
      InternalServerError("OrganizerController.findById: " + t.getMessage)
    }
  }

  def findOrganizersContaining(pattern: String) = Action.async {
    organizerMethods.findAllContaining(pattern) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.findOrganizersContaining: ", t)
      InternalServerError("OrganizerController.findOrganizersContaining: " + t.getMessage)
    }
  }

  val organizerBindingForm = Form(
    mapping(
      "facebookId" -> optional(nonEmptyText(2)),
      "name" -> nonEmptyText(2),
      "description" -> optional(nonEmptyText(2)),
      "websites" -> optional(nonEmptyText(4)),
      "imagePath" -> optional(nonEmptyText(2))
    )(organizerMethods.formApply)(organizerMethods.formUnapply)
  )

  def createOrganizer = Action.async { implicit request =>
    organizerBindingForm.bindFromRequest().fold(
      formWithErrors => Future { BadRequest(formWithErrors.errorsAsJson) },
      organizer => {
        organizerMethods.save(organizer) map { organizerCreated =>
          Ok(Json.toJson(organizerCreated))
        } recover {
          case throwable: Throwable =>
            Logger.error("OrganizerController.createOrganizer: INTERNAL_SERVER_ERROR: ", throwable)
            InternalServerError("OrganizerController.createOrganizer: " + throwable.getMessage)
        }
      }
    )
  }
//
//  def followOrganizerByOrganizerId(organizerId : Long) = SecuredAction { implicit request =>
//    organizerMethods.followById(request.identity.UUID, organizerId) match {
//      case Success(_) =>
//        Created
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
//        Conflict
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
//        NotFound
//      case Failure(unknownException) =>
//        Logger.error("OrganizerController.followOrganizerByOrganizerId", unknownException)
//        InternalServerError
//    }
//  }
//
//  def unfollowOrganizerByOrganizerId(organizerId : Long) = SecuredAction { implicit request =>
//    val userId = request.identity.UUID
//    organizerMethods.unfollowByOrganizerId(userId, organizerId) match {
//      case Success(1) =>
//        Ok
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//        Logger.error(s"The user (id: $userId) does not follow the organizer (organizerId: $organizerId) or the organizer does not exist.")
//        NotFound
//      case Failure(unknownException) =>
//        Logger.error("OrganizerController.followOrganizerByOrganizerId", unknownException)
//        InternalServerError
//    }
//  }
//
//  def followOrganizerByFacebookId(facebookId : String) = SecuredAction { implicit request =>
//    organizerMethods.followByFacebookId(request.identity.UUID, facebookId) match {
//      case Success(_) =>
//        Created
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
//        Logger.error("OrganizerController.followOrganizerByFacebookId", psqlException)
//        Conflict("This user already follow this organizer.")
//      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
//        Logger.error("OrganizerController.followOrganizerByFacebookId", psqlException)
//        NotFound("There is no organizer with this id.")
//      case Failure(unknownException) =>
//        Logger.error("OrganizerController.followOrganizerByFacebookId", unknownException)
//        InternalServerError
//    }
//  }
//
//  def getFollowedOrganizers = SecuredAction { implicit request =>
//    Ok(Json.toJson(organizerMethods.getFollowedOrganizers(request.identity.UUID)))
//  }
//
//  def isOrganizerFollowed(organizerId: Long) = SecuredAction { implicit request =>
//    Ok(Json.toJson(organizerMethods.isFollowed(request.identity.UUID, organizerId)))
//  }
//
  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action.async {
    organizerMethods.findNearCity(city, numberToReturn, offset) map { organizers =>
      Ok(Json.toJson(organizers))
    }
  }
}

