package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.User
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class Application @Inject()(ws: WSClient,
                            val messagesApi: MessagesApi,
                            val env: Environment[User, CookieAuthenticator],
                            socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] {

  def index = Action { implicit request =>
//    val userConnected: Boolean = request.identity match {
//      case Some(userConnectedValue) => true
//      case None => false
//    }
//    Ok(views.html.index(userConnected))
    Ok(views.html.index(true))
  }
//    def index = SecuredAction.async { implicit request =>
//      Future.successful(Ok)
//    }

//  /*#################### CAROUSEL ########################*/
//  def infos = Action { Ok(Json.toJson(Info.findAll())) }
//
//  def info(id: Long) = Action { Ok(Json.toJson(Info.find(id))) }
}
