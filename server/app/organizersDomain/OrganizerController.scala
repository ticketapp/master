package organizersDomain

import javax.inject.Inject

import addresses.SearchGeographicPoint
import application.{Administrator, User}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import database.{EventOrganizerRelation, UserOrganizerRelation}
import json.JsonHelper._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class OrganizerController @Inject()(ws: WSClient,
                                    val organizerMethods: OrganizerMethods,
                                    val messagesApi: MessagesApi,
                                    val env: Environment[User, CookieAuthenticator],
                                    val geographicPointMethods: SearchGeographicPoint,
                                    socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] with organizerFormsTrait with Utilities {

  def findAllSinceOffset(offset: Long, numberToReturn: Long) = Action.async {
    organizerMethods.findSinceOffset(offset = offset, numberToReturn = numberToReturn) map { organizers =>
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

  def findContaining(pattern: String) = Action.async {
    organizerMethods.findAllContaining(pattern) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.findOrganizersContaining: ", t)
      InternalServerError("OrganizerController.findOrganizersContaining: " + t.getMessage)
    }
  }

  def create = SecuredAction(Administrator()).async { implicit request =>
    organizerBindingForm.bindFromRequest().fold(
      formWithErrors => Future { BadRequest(formWithErrors.errorsAsJson) },
      organizer => {
        organizerMethods.saveWithAddress(organizer) map { organizerCreated =>
          Ok(Json.toJson(organizerCreated))
        } recover {
          case throwable: Throwable =>
            Logger.error("OrganizerController.createOrganizer: INTERNAL_SERVER_ERROR: ", throwable)
            InternalServerError("OrganizerController.createOrganizer: " + throwable.getMessage)
        }
      }
    )
  }

  def followByOrganizerId(organizerId: Long) = SecuredAction.async { implicit request =>
    organizerMethods.followByOrganizerId(
      UserOrganizerRelation(userId = request.identity.uuid, organizerId = organizerId)) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("OrganizerController.followOrganizer: organizerMethods.follow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"OrganizerController.followOrganizerByOrganizerId: $organizerId is already followed")
        Conflict
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"OrganizerController.followOrganizerByOrganizerId: there is no organizer with the id $organizerId")
        NotFound
      case unknownException =>
        Logger.error("OrganizerController.followOrganizer", unknownException)
        InternalServerError
    }
  }

  def unfollowByOrganizerId(organizerId : Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.unfollow(
      UserOrganizerRelation(userId = request.identity.uuid, organizerId = organizerId)) map {
      case 1 =>
        Ok
      case _ =>
        Logger.error("OrganizerController.unfollowOrganizer: organizerMethods.unfollow did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the organizer (organizerId: $organizerId).")
        NotFound
      case unknownException =>
        Logger.error("OrganizerController.unfollowOrganizer: unknownException: ", unknownException)
        InternalServerError
    }
  }

  def followByFacebookId(facebookId : String) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.followByFacebookId(userId, facebookId) map {
      case 1 =>
        Created
      case _ =>
        Logger.error("OrganizerController.followOrganizerByFacebookId: organizerMethods.followOrganizerByFacebookId did not return 1")
        InternalServerError
    } recover {
      case psqlException: PSQLException if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error(s"The user (id: $userId) does not follow the organizer (organizerFacebookId: $facebookId).")
        NotFound
      case psqlException: PSQLException if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error(s"The user (id: $userId) already follow organizerFacebookId: $facebookId).")
        Conflict
      case unknownException =>
        Logger.error("OrganizerController.followOrganizerByFacebookId", unknownException)
        InternalServerError
    }
  }

  def findFollowed = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    organizerMethods.findFollowedOrganizers(userId) map { organizers =>
      Ok(Json.toJson(organizers))
    } recover { case t: Throwable =>
      Logger.error("OrganizerController.getFollowedOrganizers: ", t)
      InternalServerError("OrganizerController.getFollowedOrganizers: " + t.getMessage)
    }
  }

  def isFollowed(organizerId: Long) = SecuredAction.async { implicit request =>
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

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int) = Action.async {
    geographicPointMethods.stringToTryPoint(geographicPoint) match {
      case Failure(exception) =>
        Logger.error("OrganizerController.findOrganizersNear: ", exception)
        Future(BadRequest(Json.toJson("Invalid geographicPoint")))

      case Success(point) =>
        organizerMethods.findNear(geographicPoint = point, numberToReturn = numberToReturn: Int, offset = offset: Int) map {
          organizers =>
            Ok(Json.toJson(organizers))
        } recover {
          case NonFatal(e) =>
            Logger.error("OrganizerController.findOrganizersNear: ", e)
            InternalServerError("OrganizerController.findOrganizersNear: " + e.getMessage)
        }
    }
  }

  def deleteEventRelation(eventId: Long, organizerId: Long) = SecuredAction(Administrator()).async {
    organizerMethods.deleteEventRelation(EventOrganizerRelation(eventId, organizerId)) map { result =>
      Ok(Json.toJson(result))
    }
  }

  def saveEventRelation(eventId: Long, organizerId: Long) = SecuredAction(Administrator()).async {
    organizerMethods.saveEventRelation(EventOrganizerRelation(eventId, organizerId)) map { result =>
      Ok(Json.toJson(result))
    }
  }
}

