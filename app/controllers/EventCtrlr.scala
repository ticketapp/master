package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json.Json
import models.{Image, Tariff, Event}
import json.JsonHelper._


object EventController extends Controller with securesocial.core.SecureSocial {
  def events = Action {
    Ok(Json.toJson(Event.findAll))
  }

  def eventsByPlace(placeId: Long) = Action {
    Ok(Json.toJson(Event.findAllByPlace(placeId)))
  }

  def event(id: Long) = Action {
    Event.find(id) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def eventsStartingWith(pattern: String) = Action {
    Ok(Json.toJson(Event.findAllStartingWith(pattern)))
  }

  val eventBindingForm = Form(mapping(
      "name" -> nonEmptyText(2),
      "startSellingTime" -> optional(date),
      "endSellingTime" -> optional(date),
      "description" -> nonEmptyText(2),
      "startTime" -> date,
      "endTime" -> optional(date),
      "ageRestriction" -> number,
      /*"images" -> list( mapping(
          "paths" -> text,
          "alts" -> text
        )(Image.formApply)(Image.formUnapply)),*/
      "tariffs" -> seq( mapping(
        "denominations" -> nonEmptyText,
        "nbTicketToSells" -> number,
        "prices" -> bigDecimal,
        "startTimes" -> date,
        "endTimes" -> date
      )(Tariff.formApply)(Tariff.formUnapply))
    )(Event.formApply)(Event.formUnapply)
  )
    /*"denominations" -> list(text),
  "nbTicketToSells" -> list(number),
  "prices" -> list(bigDecimal),
  "startTimes" -> list(date),
  "endTimes" -> list(date)*/


  def createEvent = Action { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      event => {
        val eventId = Event.save(event)
        //Redirect(routes.Admin.indexAdmin())
        Ok(Json.toJson(Event.find(eventId)))
      }
    )
  }

  def followEvent(userId : Long, eventId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Event.followEvent(userId, eventId)
    Redirect(routes.Admin.indexAdmin())
  }

  def findEventsInCircle(peripheral: String) = Action {
    Ok(Json.toJson(Event.findAllInCircle(peripheral)))
  }
}