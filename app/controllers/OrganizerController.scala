package controllers

import models.Organizer
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
import json.JsonHelper.organizerWrites

object OrganizerController extends Controller with securesocial.core.SecureSocial {
  def organizers(numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Organizer.findAll(numberToReturn: Int, offset: Int)))
  }

  def organizer(id: Long) = Action {
    Ok(Json.toJson(Organizer.find(id)))
  }

  def findOrganizersContaining(pattern: String) = Action {
    Ok(Json.toJson(Organizer.findAllContaining(pattern)))
  }

  def followOrganizerByOrganizerId(organizerId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Organizer.followOrganizerByOrganizerId(request.user.identityId.userId, organizerId)))
  }

  def followOrganizerByFacebookId(facebookId : String) = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(Json.toJson(Organizer.followOrganizerByFacebookId(request.user.identityId.userId, facebookId)))
  }

  val organizerBindingForm = Form(
    mapping(
      "facebookId" -> optional(nonEmptyText(2)),
      "name" -> nonEmptyText(2),
      "imagePath" -> optional(nonEmptyText(2))
    )(Organizer.formApply)(Organizer.formUnapply)
  )

  def createOrganizer = Action { implicit request =>
    organizerBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      organizer => {
        Organizer.save(organizer) match {
          case Some(eventId) => Ok(Json.toJson(Organizer.find(eventId)))
          case None => Ok(Json.toJson("The organizer couldn't be saved"))
        }
      }
    )
  }

  def findNearCity(city: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Organizer.findNearCity(city, numberToReturn, offset)))
  }
}

