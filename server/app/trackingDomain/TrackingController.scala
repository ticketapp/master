package trackingDomain

import java.util.UUID
import javax.inject.Inject

import application.User
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import json.JsonHelper._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc._
import services.LoggerHelper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.control.NonFatal


class TrackingController @Inject()(val messagesApi: MessagesApi,
                                   val env: Environment[User, CookieAuthenticator],
                                   val trackingMethods: TrackingMethods)
  extends Silhouette[User, CookieAuthenticator] with LoggerHelper{


  def findSessions() = Action.async {
    trackingMethods.findUserSessions map { userSessions =>
      Ok(Json.toJson(userSessions))
    } recover { case NonFatal(e) =>
      log(maybeThrowable = Option(e))
      InternalServerError(this.getClass + " findSessions: " + e.getMessage)
    }
  }


  def findCurrentSessions() = Action.async {
    trackingMethods.findInProgressSession map { userSessions =>
      Ok(Json.toJson(userSessions))
    } recover { case NonFatal(e) =>
      log(maybeThrowable = Option(e))
      InternalServerError(this.getClass + " findCurrentSessions: " + e.getMessage)
    }
  }

  def findActionsBySessionId(sessionId: String) = Action.async {
    trackingMethods.findUserActionBySessionId(UUID.fromString(sessionId)) map { userActions =>
      Ok(Json.toJson(userActions))
    } recover { case NonFatal(e) =>
      log(maybeThrowable = Option(e))
      InternalServerError(this.getClass + " findActionsBySessionId: " + e.getMessage)
    }
  }

  def saveUserAction = Action.async { request =>
    val userAction = request.body.asJson.get.validate[UserAction]
    trackingMethods.saveUserAction(userAction.get) map { userActions =>
      Ok(Json.toJson(userActions))
    } recover { case NonFatal(e) =>
      log(maybeThrowable = Option(e))
      InternalServerError(this.getClass + " saveUserAction: " + e.getMessage)
    }
  }

  def saveUserSession(screenWidth: Int, screenHeight: Int) = Action.async { request =>
    val ip = request.remoteAddress
    val sessionId = UUID.randomUUID()
    val newSession = UserSession(uuid = sessionId, ip = ip, screenWidth = screenWidth, screenHeight = screenHeight)
    trackingMethods.saveUserSession(newSession) map { _ =>
      Ok(Json.toJson(sessionId))
    } recover { case NonFatal(e) =>
      log(maybeThrowable = Option(e))
      InternalServerError(this.getClass + " saveUserSession: " + e.getMessage)
    }
  }
}

