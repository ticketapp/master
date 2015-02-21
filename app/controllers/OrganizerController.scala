package controllers

import controllers.EventController._
import models.{Artist, Organizer}
import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json

import scala.util.{Failure, Success}


object OrganizerController extends Controller {
  def organizers = Action {
    Ok(Json.toJson(Organizer.findAll))
  }

  def organizer(id: Long) = Action {
    Ok(Json.toJson(Organizer.find(id)))
  }

  def findOrganizersContaining(pattern: String) = Action {
    Ok(Json.toJson(Organizer.findAllContaining(pattern)))
  }

  def deleteOrganizer(organizerId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM organizers WHERE organizerId={organizerId}").on(
        'organizerId -> organizerId
      ).executeUpdate()
    }
  }

  def followOrganizer(userId : Long, organizerId : Long) = Action {
    Organizer.followOrganizer(userId, organizerId)
    Redirect(routes.Admin.indexAdmin())
  }

  val organizerBindingForm = Form(
    mapping(
      "facebookId" -> optional(nonEmptyText()),
      "name" -> nonEmptyText(2)
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

