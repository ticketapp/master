package controllers

import play.api.data.Form
import play.api.mvc._
import play.api.libs.json.Json
import models.{Info, Ticket}



object Application extends Controller with securesocial.core.SecureSocial {
  def index = UserAwareAction { implicit request =>
    Ticket.createQrCode(2)
    val userName = request.user match {
      case Some(user) => user.fullName
      case _ => "guest"
    }
    println("Hello %s".format(userName))
    Ok(views.html.index())
  }

  /*#################### CAROUSSEL ########################*/
  def infos = Action {
    Ok(Json.toJson(Info.findAll()))
  }

  def info(id: Long) = Action {
    Ok(Json.toJson(Info.find(id)))
  }
}
