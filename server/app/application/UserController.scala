package application

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.Logger
import play.api.Play.current
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc._
import services.LoggerHelper

import scala.concurrent.Future
import scala.util.control.NonFatal


class UserController @Inject() (ws: WSClient,
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                val userMethods: UserMethods)
  extends Silhouette[User, CookieAuthenticator] with LoggerHelper {

  def getTracksRemoved = SecuredAction.async { implicit request =>
    userMethods.findUUIDOfTracksRemoved(request.identity.uuid) map { response =>
      Ok(Json.toJson(response))
    } recover {
      case e =>
        Logger.error("UserController.getTracksRemoved: ", e)
        InternalServerError
    }
  }

  def isConnected = SecuredAction { implicit request =>
      Ok(Json.toJson(true))
  }


  case class Token(token: String)
  def findFacebookAccessToken = SecuredAction { implicit request =>
    val ip = request.remoteAddress
    val id = request.identity.uuid
    Future(userMethods.maybeLinkGuestUser(ip = ip, uUID = id))

    val token = Json.parse("""{"token": 1}""")
    Ok(Json.toJson(token))
  }

  def getUserGeographicPoint = Action.async { implicit request =>
    WS.url("http://ip-api.com/json/" + request.remoteAddress)
      .get()
      .map { response =>
        Ok(Json.toJson(response.json))
      } recover {
      case e =>
        Logger.error("UserController.getUserGeographicPoint: ", e)
        InternalServerError
    }
  }
}
