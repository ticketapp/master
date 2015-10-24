package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper.organizerWrites
import models._
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
import services.Utilities
import scala.concurrent.Future
import scala.util.{Failure, Success}
import json.JsonHelper._
import json.JsonHelper._

class OrganizerController @Inject()(ws: WSClient,
                                    val organizerMethods: OrganizerMethods,
                                    val messagesApi: MessagesApi,
                                    val env: Environment[User, CookieAuthenticator],
                                    val utilities: Utilities,
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

  def followOrganizerByOrganizerId(organizerId: Long) = SecuredAction.async { implicit request =>
    organizerMethods.followByOrganizerId(UserOrganizerRelation(request.identity.uuid, organizerId)) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("OrganizerController.followOrganizer: organizerMethods.follow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(s"OrganizerController.followOrganizerByOrganizerId: $organizerId is already followed")
        Conflict
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"OrganizerController.followOrganizerByOrganizerId: there is no organizer with the id $organizerId")
        NotFound
      case unknownException =>
        Logger.error("OrganizerController.followOrganizer", unknownException)
        InternalServerError
    }
  }

  def unfollowOrganizerByOrganizerId(organizerId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.unfollow(UserOrganizerRelation(userId, organizerId)) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("OrganizerController.unfollowOrganizer: organizerMethods.unfollow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the organizer (organizerId: $organizerId) or the organizer does not exist.")
        NotFound
      case unknownException =>
        Logger.error("OrganizerController.unfollowOrganizer", unknownException)
        InternalServerError
    }
  }

  def followOrganizerByFacebookId(facebookId : String) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.followByFacebookId(userId, facebookId) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("OrganizerController.followOrganizerByFacebookId: organizerMethods.followOrganizerByFacebookId did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == utilities.FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the organizer (organizerFacebookId: $facebookId) or the organizer does not exist.")
        NotFound
      case psqlException: PSQLException if psqlException.getSQLState == utilities.UNIQUE_VIOLATION =>
        Logger.error(s"The user (id: $userId) already follow organizerFacebookId: $facebookId).")
        Conflict
      case unknownException =>
        Logger.error("OrganizerController.followOrganizerByFacebookId", unknownException)
        InternalServerError
    }
  }

  def getFollowedOrganizers = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.getFollowedOrganizers(userId) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.getFollowedOrganizers: ", t)
      InternalServerError("OrganizerController.getFollowedOrganizers: " + t.getMessage)
    }
  }

  def isOrganizerFollowed(organizerId: Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.isFollowed(UserOrganizerRelation(userId, organizerId)) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.isOrganizerFollowed: ", t)
      InternalServerError("OrganizerController.isOrganizerFollowed: " + t.getMessage)
    }
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action.async {
    organizerMethods.findNearCity(city, numberToReturn, offset) map { organizers =>
      Ok(Json.toJson(organizers))
    }
  }
}

