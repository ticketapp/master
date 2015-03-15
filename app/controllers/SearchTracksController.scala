package controllers

import play.api.libs.iteratee.Enumerator
import play.api.mvc.Controller
import scala.concurrent.Future
import models.{Artist, Track}
import json.JsonHelper._
import play.api.libs.ws.{WS, Response}
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities.{ normalizeUrl }
import services.SearchSoundCloudTracks._
import services.SearchYoutubeTracks._
import controllers.SearchArtistsController._

object SearchTracksController extends Controller {
  def getTracksForArtist(pattern: String) = Action.async {
    getEventuallyFacebookArtists(pattern).map { facebookArtists =>
      val soundCloudTracksEnumerator = Enumerator.flatten(
        getSoundCloudTracksForArtist(facebookArtists(0)).map { soundCloudTracks =>
          Enumerator(Json.toJson(soundCloudTracks))
        }
      )

      val youtubeTracksEnumerator = Enumerator.flatten(
        getYoutubeTracksForArtist(facebookArtists(0), pattern).map {
          youtubeTracks =>
          Enumerator(Json.toJson(youtubeTracks))
        }
      )

      val enumerators = Enumerator.interleave(
        Enumerator(Json.toJson(facebookArtists)), soundCloudTracksEnumerator, youtubeTracksEnumerator
      )
      Ok.chunked(youtubeTracksEnumerator)
    }
  }
}
