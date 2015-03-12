package controllers

import play.api.mvc._
import play.api.libs.json.Json
import models.{Info, Ticket}
import json.JsonHelper._

object Application extends Controller with securesocial.core.SecureSocial {
  def index = UserAwareAction { implicit request =>
    val userConnected: Boolean = request.user match {
      case Some(userConnected) => true
      case _ => false
    }
    //println("Yo %s la sacoche".format(userName))
    Ok(views.html.index(userConnected))
  }

  def getGeographicPoint = {

  }

  /*#################### CAROUSEL ########################*/
  def infos = Action {
    Ok(Json.toJson(Info.findAll()))
  }

  def info(id: Long) = Action {
    Ok(Json.toJson(Info.find(id)))
  }
}
