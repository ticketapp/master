package controllers

import models.Track
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._

object TrackController extends Controller {
  val trackBindingForm = Form(
    mapping(
      "artistFacebookUrl" -> nonEmptyText(2),
      "track" -> mapping(
        "title" -> nonEmptyText(2),
        "url" -> nonEmptyText(3),
        "platform" -> nonEmptyText(3),
        "thumbnailUrl" -> nonEmptyText(2)
      )(Track.formApply)(Track.formUnapply)
    )(Track.formWithArtistIdApply)(Track.formWithArtistIdUnapply)
  )

  def createTrack = Action { implicit request =>
    trackBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      artistIdAndTrack => {
        Track.saveTrackAndArtistRelation(artistIdAndTrack.track, Right(artistIdAndTrack.artistFacebookUrl))
        match {
          case Some(trackId) => Ok(Json.toJson(Track.find(trackId)))
          case None => Ok(Json.toJson("The track couldn't be saved"))
        }
      }
    )
  }
}