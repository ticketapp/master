package trackingDomain

import java.util.UUID
import javax.inject.Inject

import application.User
import com.mohiva.play.silhouette.api.{Silhouette, Environment}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc._
import json.JsonHelper._
import org.joda.time.DateTime
import tariffsDomain.Tariff
import ticketsDomain.{SalableEvent, TicketMethods}
import trackingDomain.TrackingMethods
import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Success, Failure}


class TrackingController @Inject()(val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 val trackingMethods: TrackingMethods)
  extends Silhouette[User, CookieAuthenticator] {


  def findSessions() = Action.async {
    trackingMethods.findUserSessions map { userSessions =>
      Ok(Json.toJson(userSessions))
    } recover { case t: Throwable =>
      //replace by custom logger
      Logger.error("TrackingController.findSessions: ", t)
      InternalServerError("TrackingController.findSessions: " + t.getMessage)
    }
  }

  def findActionsBySessionId(sessionId: String) = Action.async {
    trackingMethods.findUserActionBySessionId(UUID.fromString(sessionId)) map { userActions =>
      Ok(Json.toJson(userActions))
    } recover { case t: Throwable =>
      //replace by custom logger
      Logger.error("TrackingController.findActionsBySessionId: ", t)
      InternalServerError("TrackingController.findActionsBySessionId: " + t.getMessage)
    }
  }

  def saveUserAction() = Action.async { request =>
    val userAction = request.body.asJson.get.validate[UserAction](userActionReads)
    trackingMethods.saveUserAction(userAction.get) map { userActions =>
      Ok(Json.toJson(userActions))
    } recover { case t: Throwable =>
      //replace by custom logger
      Logger.error("TrackingController.saveUserAction: ", t)
      InternalServerError("TrackingController.saveUserAction: " + t.getMessage)
    }
  }

  def saveUserSession() = Action.async { request =>
    val ip = request.remoteAddress
    val sessionId = UUID.randomUUID()
    trackingMethods.saveUserSession(UserSession(id = sessionId, ip = ip)) map { userSession =>
      Ok(Json.toJson(sessionId))
    } recover { case t: Throwable =>
      //replace by custom logger
      Logger.error("TrackingController.saveUserSession: ", t)
      InternalServerError("TrackingController.saveUserSession: " + t.getMessage)
    }
  }


  }

