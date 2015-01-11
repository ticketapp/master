package controllers

import play.api.data.Form
import play.api.mvc._
import play.api.libs.json.Json
import models.{Info, Ticket}


object Application extends Controller {
  def index = Action {
    Ticket.createQrCode(2)
    Ok(views.html.index())
  }

  ////////////////// User system ///////////////////
  def login = TODO/*Action { implicit request =>
    Ok(views.html.login(user, loginForm))
  }*/

  def logout = TODO/*Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }*/

  def authenticate = TODO /*Action { implicit request =>
    val requestFrom: Form[Credentials] = loginForm.bindFromRequest()
    requestFrom.fold(
    formWithErrors => BadRequest(views.html.login(user, formWithErrors)),
    {case (credentials) => {
      try {
        val user = UserDao.login(credentials)
        Ok(indexView(Option.apply(user)))
          .withSession(
            "user.id" -> user.id.toString,
            "user.profile" -> user.profile.toString,
            "user.email" -> user.email,
            "user.login" -> user.login)
      }
      catch{
        case x:UserNotFoundException => Results.Redirect(routes.Application.login)
        case x:InvalidCredentialsException => Results.Redirect(routes.Application.login)
      }
    }}
    )
  }
*/
  /*#################### CAROUSSEL ########################*/
  def infos = Action {
    Ok(Json.toJson(Info.findAll()))
  }

  def info(id: Long) = Action {
    Ok(Json.toJson(Info.find(id)))
  }
}
