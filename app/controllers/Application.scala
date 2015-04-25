package controllers

import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json.Json
import models.{Info, Ticket}
import json.JsonHelper._
import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller with securesocial.core.SecureSocial {
  def index = UserAwareAction { implicit request =>
    val userConnected: Boolean = request.user match {
      case Some(userConnectedValue) => true
      case _ => false
    }
    Ok(views.html.index(userConnected))
  }

  /*#################### CAROUSEL ########################*/
  def infos = Action {
    Ok(Json.toJson(Info.findAll()))
  }

  def info(id: Long) = Action {
    Ok(Json.toJson(Info.find(id)))
  }
}
