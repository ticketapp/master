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
import services.SearchSoundCloudTracks.getSoundCloudTracksForArtist
import services.SearchYoutubeTracks.getYoutubeTracksForArtist

import scala.concurrent.Future


object ArtistController extends Controller with securesocial.core.SecureSocial {
  def artists = Action {
    Ok(Json.toJson(Artist.findAll()))
  }

  def artist(artistId: Long) = Action {
    Ok(Json.toJson(Artist.find(artistId)))
  }

  def artistByFacebookUrl(facebookUrl: String) = Action {
    Artist.findByFacebookUrl(facebookUrl) match {
      case Some(artist) => Ok(Json.toJson(artist))
      case None =>  Ok
    }
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
          )(Track.formApplyForTrackCreatedWithArtist)(Track.formUnapplyForTrackCreatedWithArtist)
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
          Future { Artist.save(patternAndArtist.artist) }
          Ok.chunked(getArtistTracks(patternAndArtist))
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def getArtistTracks(patternAndArtist: PatternAndArtist) = {
    val soundCloudTracksEnumerator = Enumerator.flatten(
      getSoundCloudTracksForArtist(patternAndArtist.artist).map { soundCloudTracks =>
        Enumerator(Json.toJson(soundCloudTracks))
      }
    )
    val youtubeTracksEnumerator = Enumerator.flatten(
      getYoutubeTracksForArtist(
        patternAndArtist.artist.name, patternAndArtist.artist.facebookId.get, patternAndArtist.searchPattern)
        .map { youtubeTracks =>
          Enumerator(Json.toJson(youtubeTracks))
        }
    )
    Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator)
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
