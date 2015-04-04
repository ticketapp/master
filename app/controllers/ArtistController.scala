package controllers

import json.JsonHelper._
import models.Artist.PatternAndArtist
import models._
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
  def artists = Action { Ok(Json.toJson(Artist.findAll)) }

  def artistsSinceOffsetBy(number: Int, offset: Int) = Action {
    Ok(Json.toJson(Artist.findSinceOffset(number, offset)))
  }

  def artist(artistId: Long) = Action {
    Artist.find(artistId) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def artistByFacebookUrl(facebookUrl: String) = Action {
    Artist.findByFacebookUrl(facebookUrl) match {
      case Some(artist) => Ok(Json.toJson(artist))
      case None =>  Ok
    }
  }

  def artistsByGenre(genre: String, numberToReturn: Int, offset: Int) = Action {
    Ok(Json.toJson(Artist.findByGenre(genre, numberToReturn, offset)))
  }

  def findArtistsContaining(pattern: String) = Action {
    Ok(Json.toJson(Artist.findAllContaining(pattern)))
  }

  def eventsByArtist(facebookUrl: String) = Action {
    Ok(Json.toJson(Event.findAllByArtist(facebookUrl)))
  }

  val artistBindingForm = Form(
    mapping(
      "searchPattern" -> nonEmptyText(3),
      "artist" -> mapping(
        "facebookId" -> optional(nonEmptyText(2)),
        "artistName" -> nonEmptyText(2),
        "imagePath" -> optional(nonEmptyText(2)),
        "description"  -> optional(nonEmptyText),
        "facebookUrl"  -> nonEmptyText,
        "websites" -> seq(nonEmptyText(4)),
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
            "avatarUrl" -> optional(nonEmptyText),
            "artistFacebookUrl" -> nonEmptyText(2),
            "redirectUrl" -> optional(nonEmptyText(2))
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
          Artist.save(patternAndArtist.artist)
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
      getYoutubeTracksForArtist(patternAndArtist.artist, patternAndArtist.searchPattern)
        .map { youtubeTracks => Enumerator(Json.toJson(youtubeTracks + Track(Option(-2L), "", "", "", "", ""))) }
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
