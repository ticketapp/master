package others

import application.User
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import json.JsonHelper._
import javax.inject.Inject
import play.api.libs.ws._
import javax.inject.Inject
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.MessagesApi

class MailController @Inject()(ws: WSClient,
                               val messagesApi: MessagesApi,
                               val env: Environment[User, CookieAuthenticator],
                               socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

//  def mails = Action { Ok(Json.toJson(Mail.findAll)) }
//
//  private val mailBindingForm = Form(mapping(
//    "subject" -> nonEmptyText(3),
//    "message" -> nonEmptyText(8)
//  )(Mail.mailFormApply)(Mail.mailFormUnapply))
//
//  def create = SecuredAction { implicit request =>
//    mailBindingForm.bindFromRequest().fold(
//      formWithErrors => {
//        Logger.error("MailController.create:" + formWithErrors.errorsAsJson)
//        BadRequest(formWithErrors.errorsAsJson)
//      },
//      mail => { Ok(Json.toJson(Mail.save(mail.copy(userId = Option(request.identity.UUID))))) }
//    )
//  }
}
