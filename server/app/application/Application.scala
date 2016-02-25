package application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.ws._
import play.api.mvc.Action
import userDomain.{UserMethods, Administrator, GuestUser, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(ws: WSClient,
                            val messagesApi: MessagesApi,
                            val userMethods: UserMethods,
                            val env: Environment[User, CookieAuthenticator],
                            socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] with ConnectionTrait {

//  def index = UserAwareAction { implicit request =>
//    val userConnected: Boolean = request.identity match {
//      case Some(userConnectedValue) =>
//        Logger.info("connected" + userConnectedValue)
//        true
//      case None =>
//        Logger.info("not connected")
//        false
//    }
//    Ok(views.html.index(userConnected))
//  }


  def index = Action { implicit request =>
    userMethods.findGuestUserByIp(request.remoteAddress) flatMap {
      case Some(guest) =>
        Future(None)
      case _ =>
        userMethods.saveGuestUser(GuestUser(request.remoteAddress, None))
    }
    Ok(views.html.landingPage())
  }

  def claude = SecuredAction(Administrator()) { implicit request =>
    Ok(views.html.index(false))
  }

  def admin = SecuredAction(Administrator()) { implicit request =>
    Ok(views.html.admin.indexAdmin())
  }

  def googleValidation = UserAwareAction { implicit request =>
    Ok(views.html.googlea30e62044bb92d88())
  }

  def signOut = SecuredAction.async { implicit request =>
    val result = Redirect(routes.Application.index())
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }

  def signIn = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) =>
        Logger.info("Already connected")
        NotModified
      case None =>
        signInForm.bindFromRequest().fold(
          formWithErrors => {
            Logger.error("signIn: " + formWithErrors.errorsAsJson)
            BadRequest(formWithErrors.errorsAsJson)
          },
          userInfo => {
            Logger.error("signIn as: " + userInfo)
            Ok
          }
        )
    }
  }
}
