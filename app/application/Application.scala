package application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.ws._


class Application @Inject()(ws: WSClient,
                            val messagesApi: MessagesApi,
                            val global: Global,
                            val env: Environment[User, CookieAuthenticator],
                            socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] with ConnectionTrait {

  def index = UserAwareAction { implicit request =>
    val userConnected: Boolean = request.identity match {
      case Some(userConnectedValue) =>
        Logger.info("connected" + userConnectedValue)
        true
      case None =>
        Logger.info("not connected")
        false
    }
    Ok(views.html.index(userConnected))
  }
  def googleValidation = UserAwareAction { implicit request =>
    Ok(views.html.googlea30e62044bb92d88)
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
