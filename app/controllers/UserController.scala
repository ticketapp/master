package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models.{Tool, User}
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc._

class UserController @Inject() (ws: WSClient,
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {
/*

  def users = Action {
    Ok(Json.toJson(User.findAll()))
  }

  def user(id: Long) = Action {
    Ok(Json.toJson(User.find(id)))
  }

  def findUsersContaining(pattern: String) = Action {
    Ok(Json.toJson(User.findAllContaining(pattern)))
  }

  def findToolsByUserId(userId: Long) = Action {
    Ok(Json.toJson(Tool.findByUserId(userId)))
  }

  val toolsBindingForm = Form(mapping(
    "tools" -> nonEmptyText(2),
    "userId" -> longNumber()
  )(Tool.formApply)(Tool.formUnapply))

  val userBindingForm = Form(mapping(
    "email" -> email,
    "nickname" -> nonEmptyText(2),
    "password" -> nonEmptyText(2),
    "profile" -> nonEmptyText(2)
  )(User.formApply)(User.formUnapply)
  )

  def createUser = Action { implicit request =>
    userBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      user => {
        User.save
        Redirect(routes.UserController.user(1))
      }
    )
  }

  def findFacebookAccessToken = SecuredAction { implicit request =>
    Ok(Json.toJson(User.findFacebookAccessToken(request.identity.UUID)))
  }

  def getUserGeographicPoint = Action.async { request =>
    WS.url("http://ip-api.com/json/" + request.remoteAddress)
      .get()
      .map { response =>
      Ok(Json.toJson(response.json))
    }
  }

  def getTracksRemoved = SecuredAction { implicit request =>
    Ok(Json.toJson(User.getTracksRemoved(request.identity.UUID)))
   }*/
}
