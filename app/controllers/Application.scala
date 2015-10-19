package controllers

import javax.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import com.mohiva.play.silhouette.api.{LogoutEvent, Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.User
import play.api.i18n.MessagesApi
import play.api.libs.ws._

import scala.concurrent.Future

class Application @Inject()(ws: WSClient,
                            val messagesApi: MessagesApi,
                            val env: Environment[User, CookieAuthenticator],
                            socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] {

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

  def signOut = SecuredAction.async { implicit request =>
    val result = Redirect(routes.Application.index())
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }


  //  def createEvent = Action { implicit request =>
  //    eventBindingForm.bindFromRequest().fold(
  //      formWithErrors => {
  //        Logger.error("EventController.createEvent: " + formWithErrors.errorsAsJson)
  //        BadRequest(formWithErrors.errorsAsJson)
  //      },
  //      event =>
  //        eventMethods.save(event) match {
  //          case Some(eventId) => Ok(Json.toJson(eventMethods.find(eventId)))
  //          case None => InternalServerError
  //        }
  //    )
  //  }

  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(Data.apply)(Data.unapply)
  )

  case class Data(email: String, password: String, rememberMe: Boolean)

  def signIn = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) =>
        Logger.info("Already connected")
        Ok
      case None =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Logger.error("signIn: " + formWithErrors.errorsAsJson)
            BadRequest(formWithErrors.errorsAsJson)
          },
          okk => {
            println(okk)
            Ok
          }
        )
    }
  }

  val form2 = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(Data2.apply)(Data2.unapply))

  case class Data2(firstName: String,
                   lastName: String,
                   email: String,
                   password: String)

  def signUp = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) =>
        Ok
      case None =>
        form2.bindFromRequest().fold(
          formWithErrors => {
            Logger.error("signUp: " + formWithErrors.errorsAsJson)
            BadRequest(formWithErrors.errorsAsJson)
          },
          okk => {
            println(okk)
            Ok
          }
        )
    }
  }

  //  /*#################### CAROUSEL ########################*/
//  def infos = Action { Ok(Json.toJson(Info.findAll())) }
//
//  def info(id: Long) = Action { Ok(Json.toJson(Info.find(id))) }
}
