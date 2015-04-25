package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json.Json
import models.{Tool, User}
import play.api.libs.concurrent.Execution.Implicits._

object UserController extends Controller with securesocial.core.SecureSocial {
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

  def createTools = Action { implicit request =>
    userBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      user => {
        User.save(user)
        Redirect(routes.UserController.user(1))
      }
    )
  }

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
        User.save(user)
        Redirect(routes.UserController.user(1))
      }
    )
  }

  def findFacebookAccessToken = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(User.findFacebookAccessToken(request.user.identityId.userId)))
  }

  def getUserGeographicPoint = Action.async { request =>
    WS.url("http://ip-api.com/json/" + request.remoteAddress)
      .get()
      .map { response =>
      Ok(Json.toJson(response.json))
    }
  }
}
