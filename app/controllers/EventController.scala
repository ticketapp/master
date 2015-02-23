package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json.Json
import models.{Image, Tariff, Event, Address}
import json.JsonHelper._


object EventController extends Controller with securesocial.core.SecureSocial {
  def events = Action {
    Ok(Json.toJson(Event.findAll))
  }

  def eventsByPlace(placeId: Long) = Action {
    Ok(Json.toJson(Event.findAllByPlace(placeId)))
  }

  def eventsByOrganizer(organizerId: Long) = Action {
    Ok(Json.toJson(Event.findAllByOrganizer(organizerId)))
  }

  def event(id: Long) = Action {
    Event.find(id) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def findEventsContaining(pattern: String, center: String) = Action {
    Ok(Json.toJson(Event.findAllContaining(pattern, center)))
  }

  def findEventsByCity(pattern: String) = Action {
    Ok(Json.toJson(Event.findAllByCityPattern(pattern)))
  }

  val eventBindingForm = Form(
    mapping(
      "name" -> nonEmptyText(2),
      "geographicPoint" -> optional(nonEmptyText(3)),
      "description" -> optional(nonEmptyText(2)),
      "startTime" -> date("yyyy-MM-dd HH:mm"),
      "endTime" -> optional(date("yyyy-MM-dd HH:mm")),
      "ageRestriction" -> number,
      "images" -> list( mapping(
          "paths" -> nonEmptyText
        )(Image.formApply)(Image.formUnapply)),
      "tariffs" -> list(
        mapping(
          "denominations" -> nonEmptyText,
          "nbTicketToSells" -> number,
          "prices" -> bigDecimal,
          "startTimes" -> date("yyyy-MM-dd HH:mm"),
          "endTimes" -> date("yyyy-MM-dd HH:mm")
        )(Tariff.formApply)(Tariff.formUnapply)),
      "addresses" -> list(
        mapping(
          "cities" -> text(2),
          "zips" -> text(2),
          "streets" -> text(2)
        )(Address.formApply)(Address.formUnapply))
    )(Event.formApply)(Event.formUnapply)
  )

  def createEvent = Action { implicit request =>
    eventBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      event => {
        Event.save(event) match {
          case Some(eventId) => Ok(Json.toJson(Event.find(eventId)))
          case None => Ok(Json.toJson("The event couldn't be saved"))
        }
      }
    )
  }

  def followEvent(eventId : Long) = SecuredAction { implicit request =>
    Event.followEvent(request.user.identityId.userId, eventId)
    //Redirect(request.getHeader("referer"))
    Ok
  }

  def findEventsInCircle(peripheral: String) = Action {
    Ok(Json.toJson(Event.findAllInCircle(peripheral)))
  }
}