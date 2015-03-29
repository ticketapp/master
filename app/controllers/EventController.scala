package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json.Json
import models.{Image, Tariff, Event, Address}
import json.JsonHelper._

import scala.util.matching.Regex

object EventController extends Controller with securesocial.core.SecureSocial {
  val geographicPointPattern = play.Play.application.configuration.getString("regex.geographicPointPattern").r

  def events(offset: Int, geographicPoint: String) = Action {
    geographicPoint match {
      case geographicPointPattern(_) => Ok(Json.toJson(Event.find20Since(offset, geographicPoint)))
      case _ => Ok(Json.toJson("Invalid geographicPoint"))
    }
  }

  def eventsWithMaxStartTime(offset: Int, geographicPoint: String, hourInterval: Int) = Action {
    geographicPoint match {
      case geographicPointPattern(_) =>
        Ok(Json.toJson(Event.find20InHourIntervalWithOffsetNearCenterPoint(offset, geographicPoint, hourInterval)))
      case _ =>
        Ok(Json.toJson("Invalid geographicPoint"))
    }
  }


  def eventsByPlace(placeId: Long) = Action {
    Ok(Json.toJson(Event.findAllByPlace(placeId)))
  }

  def eventsByOrganizer(organizerId: Long) = Action {
    Ok(Json.toJson(Event.findAllByOrganizer(organizerId)))
  }

  def event(id: Long) = Action {
    Event.find(id) match {
      case Some(event) => Ok(Json.toJson(event))
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
      "images" -> list(
        mapping(
          "path" -> nonEmptyText
        )(Image.formApply)(Image.formUnapply)),
      "tariffs" -> list(
        mapping(
          "denomination" -> nonEmptyText,
          "nbTicketToSell" -> number,
          "price" -> bigDecimal,
          "startTime" -> date("yyyy-MM-dd HH:mm"),
          "endTime" -> date("yyyy-MM-dd HH:mm")
        )(Tariff.formApply)(Tariff.formUnapply)),
      "addresses" -> list(
        mapping(
          "city" -> optional(text(2)),
          "zip" -> optional(text(2)),
          "street" -> optional(text(2))
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

  def followEvent(eventId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Event.followEvent(request.user.identityId.userId, eventId)
    //Redirect(request.getHeader("referer"))
    Ok
  }

  def findEventsInCircle(peripheral: String) = Action {
    Ok(Json.toJson(Event.findAllInCircle(peripheral)))
  }
}