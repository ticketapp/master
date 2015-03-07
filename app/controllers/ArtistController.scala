package controllers

import json.JsonHelper._
import models.Artist.PatternAndArtist
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
          println("ok")
          Ok.chunked(getArtistIdAndTracks(patternAndArtist))
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def getArtistIdAndTracks(patternAndArtist: PatternAndArtist) = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
        println(Json.toJson(soundCloudTracks))
        Enumerator(Json.toJson(soundCloudTracks))
      }
    )
    val youtubeTracksEnumerator = Enumerator.flatten(
      getYoutubeTracksForArtist(patternAndArtist.artist.name, patternAndArtist.artist.facebookId.get,
        patternAndArtist.searchPattern).map {
        youtubeTracks =>
          println(Json.toJson(youtubeTracks))
          Enumerator(Json.toJson(youtubeTracks))
      }
    )
    val artistDatabaseIdEnumerator = {
      val artistId = Artist.save(patternAndArtist.artist)
        println(artistId)
        Enumerator(Json.toJson(artistId))
      }

    Enumerator.interleave(artistDatabaseIdEnumerator, soundCloudTracksEnumerator, youtubeTracksEnumerator)
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
