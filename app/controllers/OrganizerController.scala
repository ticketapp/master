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
  def organizers = Action {
    Ok(Json.toJson(Organizer.findAll))
  }

  def organizer(id: Long) = Action {
    Ok(Json.toJson(Organizer.find(id)))
  }

  def findOrganizersContaining(pattern: String) = Action {
    Ok(Json.toJson(Organizer.findAllContaining(pattern)))
  }

  def deleteOrganizer(organizerId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM organizers WHERE organizerId = {organizerId}")
        .on('organizerId -> organizerId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot delete Organizer: " + e.getMessage)
  }

  def followOrganizer(organizerId : Long) = SecuredAction(ajaxCall = true) { implicit request =>
    Organizer.followOrganizer(request.user.identityId.userId, organizerId)
    Ok
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
}

