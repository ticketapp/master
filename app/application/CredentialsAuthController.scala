package application

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import play.api.Configuration
import play.api.i18n.MessagesApi

import scala.language.postfixOps

class CredentialsAuthController @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock)
  extends Silhouette[User, CookieAuthenticator] {


  val signInForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String, password: String, rememberMe: Boolean)

//  def authenticate = Action.async { implicit request =>
//    signInForm.bindFromRequest.fold(
//      formWithErrors => {
//        Logger.error("authenticate: " + formWithErrors.errorsAsJson)
//        Future.successful(BadRequest(formWithErrors.errorsAsJson))
//      },
//      data => {
//        val credentials = Credentials(data.email, data.password)
//        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
//          val result = Redirect(routes.Application.index())
//          userService.retrieve(loginInfo).flatMap {
//            case Some(user) =>
//              val c = configuration.underlying
//              env.authenticatorService.create(loginInfo).map {
//                case authenticator if data.rememberMe =>
//                  authenticator.copy(
//                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
//                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
//                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
//                  )
//                case authenticator => authenticator
//              }.flatMap { authenticator =>
//                env.eventBus.publish(LoginEvent(user, request, request2Messages))
//                env.authenticatorService.init(authenticator).flatMap { v =>
//                  env.authenticatorService.embed(v, result)
//                }
//              }
//            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
//          }
//        }.recover {
//          case e: ProviderException =>
//            Redirect(routes.CredentialsAuthController.authenticate()).flashing("error" -> Messages("invalid.credentials"))
//        }
//      }
//    )
//  }
}
