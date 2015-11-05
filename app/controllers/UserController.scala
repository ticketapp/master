package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models.{User, UserMethods}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc._

class UserController @Inject() (ws: WSClient,
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                val userMethods: UserMethods,
                                socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def getTracksRemoved = SecuredAction.async { implicit request =>
    userMethods.findUUIDOfTracksRemoved(request.identity.uuid) map { response =>
      Ok(Json.toJson(response))
    } recover {
      case e =>
        Logger.error("UserController.getTracksRemoved: ", e)
        InternalServerError
    }
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
