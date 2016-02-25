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
import userDomain.User

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

}
