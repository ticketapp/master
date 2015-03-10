package controllers

import models.Track
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._

object TrackController extends Controller {
 /* val trackBindingForm = Form(
    mapping(
      "name" -> nonEmptyText(2),
      "geographicPoint" -> optional(nonEmptyText(3)),
      "description" -> optional(nonEmptyText(2))
    )(Track.formApply)(Track.formUnapply)
  )

  def createTrack = Action { implicit request =>
    trackBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      track => {
        Track.save(track) match {
          case Some(trackId) => Ok(Json.toJson(Track.find(trackId)))
          case None => Ok(Json.toJson("The track couldn't be saved"))
        }
      }
    )
  }*/
}