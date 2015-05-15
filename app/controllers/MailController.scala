package controllers

import models.Mail
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import json.JsonHelper._

object MailController extends Controller with securesocial.core.SecureSocial {
  def mails = Action { Ok(Json.toJson(Mail.findAll)) }

  private val mailBindingForm = Form(mapping(
    "subject" -> nonEmptyText(3),
    "message" -> nonEmptyText(8)
  )(Mail.mailFormApply)(Mail.mailFormUnapply))

  def create = SecuredAction(ajaxCall = true) { implicit request =>
    mailBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      mail => { Ok(Json.toJson(Mail.save(mail.copy(userId = Option(request.user.identityId.userId))))) }
    )
  }
}
