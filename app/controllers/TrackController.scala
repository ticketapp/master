package controllers

import models.Track
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._


object TrackController extends Controller {

  /*

  val artistBindingForm = Form(
    mapping(
      "searchPattern" -> nonEmptyText(3),
      "artist" -> mapping(
        "facebookId" -> optional(nonEmptyText(2)),
        "artistName" -> nonEmptyText(2),
        "description"  -> optional(nonEmptyText),
        "facebookUrl"  -> optional(nonEmptyText),
        "websites" -> seq(nonEmptyText(4)),
        "images" -> seq(
          mapping(
            "path" -> nonEmptyText
          )(Image.formApply)(Image.formUnapply)),
        "genres" -> seq(
          mapping(
            "name" -> nonEmptyText
          )(Genre.formApply)(Genre.formUnapply)),
        "tracks" -> seq(
          mapping(
            "title" -> nonEmptyText,
            "url" -> nonEmptyText,
            "platform" -> nonEmptyText,
            "thumbnail" -> optional(nonEmptyText),
            "avatarUrl" -> optional(nonEmptyText)
          )(Track.formApplyForTrackCreatedWithArtist)(Track.formUnapplyForTrackCreatedWithArtist)
        )
      )(Artist.formApply)(Artist.formUnapply)
    )(Artist.formWithPatternApply)(Artist.formWithPatternUnapply)
  )

   */
  val trackBindingForm = Form(
    mapping(
      "artistId" -> longNumber,
      "track" -> mapping(
        "title" -> nonEmptyText(2),
        "url" -> nonEmptyText(3),
        "platform" -> nonEmptyText(3),
        "thumbnailUrl" -> nonEmptyText(2)
      )(Track.formApply)(Track.formUnapply)
    )(Track.formWithArtistIdApply)(Track.formWithArtistIdUnapply)
  )
/*
 def createArtist = Action { implicit request =>
    try {
      artistBindingForm.bindFromRequest().fold(
        formWithErrors => {
          println(formWithErrors.errorsAsJson)
          BadRequest(formWithErrors.errorsAsJson)
        },
        patternAndArtist => {
          Artist.save(patternAndArtist.artist)
          Ok.chunked(getArtistTracks(patternAndArtist))
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }
 */
  def createTrack = Action { implicit request =>
    trackBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      artistIdAndTrack => {
        Track.saveTrackAndArtistRelation(artistIdAndTrack.track, Left(artistIdAndTrack.artistId)) match {
          case Some(trackId) => Ok(Json.toJson(Track.find(trackId)))
          case None => Ok(Json.toJson("The track couldn't be saved"))
        }
      }
    )
  }
}