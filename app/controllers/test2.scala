package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Failure, Success, Try}

case class Contact(firstname: String,
                   informations: Seq[ContactInformation])

case class ContactInformation(label: String)

object Test2 extends Controller {
  val contactForm: Form[Contact] = Form(
    // Defines a mapping that will handle Contact values
    mapping(
      "firstname" -> nonEmptyText,
      // Defines a repeated mapping
      "informations" -> seq(
        mapping(
          "label" -> nonEmptyText
        )(ContactInformation.apply)(ContactInformation.unapply)
      )
    )(Contact.apply)(Contact.unapply)
  )

  def saveContact = Action { implicit request =>
    contactForm.bindFromRequest.fold(
      formWithErrors => Ok("error"),
      contact => {
        println(contact)
        Ok("Done")
      }
    )
  }

  def test2 = Action {
    Ok("Okay\n")
  }
}

