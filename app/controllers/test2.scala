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

case class DateTest(date: Date)

object Test2 extends Controller {
  val dateForm: Form[DateTest] = Form(
    mapping(
      "date" -> date("yyyy-MM-dd HH:mm:ss")
    )(DateTest.apply)(DateTest.unapply)
  )

  def saveDateTest = Action { implicit request =>
    dateForm.bindFromRequest.fold(
      formWithErrors => Ok("error"),
      dateTest => {
        println(dateTest)
        Ok("Done")
      }
    )
  }

  /*val contactForm: Form[Contact] = Form(
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
  }*/

  def test2 = Action {
    Ok("Okay\n")
  }
}

