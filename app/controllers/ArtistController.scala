package controllers

import json.JsonHelper._
import models.{Genre, Image, Artist, Track}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import services.Utilities.normalizeString
import services.SearchSoundCloudTracks.getSoundCloudTracksForArtist
import services.SearchYoutubeTracks.getYoutubeTracksForArtist


object ArtistController extends Controller with securesocial.core.SecureSocial {
  def artists = Action {
    Ok(Json.toJson(Artist.findAll()))
  }

  def artist(artistId: Long) = Action {
    Ok(Json.toJson(Artist.find(artistId)))
  }

  def findArtistsContaining(pattern: String) = Action {
    Ok(Json.toJson(Artist.findAllContaining(pattern)))
  }

  val artistBindingForm = Form(
    mapping(
      "pattern" -> nonEmptyText(3),
      "artist" -> mapping(
        "facebookId" -> optional(nonEmptyText(2)),
        "artistName" -> nonEmptyText(2),
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
          )(Track.formApply)(Track.formUnapply)
        )
      )(Artist.formApply)(Artist.formUnapply)
    )(Artist.formWithPatternApply)(Artist.formWithPatternUnapply)
  )

  def createArtist = Action { implicit request =>
    try {
      artistBindingForm.bindFromRequest().fold(
        formWithErrors => {
          println(formWithErrors.errorsAsJson)
          BadRequest(formWithErrors.errorsAsJson)
        },
        patternAndArtist => {
          val soundCloudTracksEnumerator = Enumerator.flatten(
            getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
              Enumerator(Json.toJson(soundCloudTracks))
            }
          )
          val youtubeTracksEnumerator = Enumerator.flatten(
            getYoutubeTracksForArtist(patternAndArtist.artist.name, patternAndArtist.artist.facebookId.get,
              patternAndArtist.pattern).map {
              youtubeTracks =>
                Enumerator(Json.toJson(youtubeTracks))
            }
          )

          val artistDatabaseIdEnumerator = Enumerator(Json.toJson(Artist.save(patternAndArtist.artist)))

          val enumerators = Enumerator.interleave(
            artistDatabaseIdEnumerator, soundCloudTracksEnumerator, youtubeTracksEnumerator
          )
          Ok.chunked(enumerators)
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def deleteArtist(artistId: Long) = Action {
    Artist.deleteArtist(artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  def followArtist(artistId : Long) = Action {
    //Artist.followArtist(userId, artistId)
    Redirect(routes.Admin.indexAdmin())
  }
}
