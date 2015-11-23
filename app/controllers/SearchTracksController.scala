package controllers

import javax.inject.Inject

import json.JsonHelper._
import models.{Artist, TrackMethods}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc.{Controller, _}
import services.{SearchYoutubeTracks, Utilities}

import scala.concurrent.Future


class SearchTracksController @Inject()(val trackMethods: TrackMethods,
                                       val searchYoutubeTracks: SearchYoutubeTracks) extends Controller with Utilities {

  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String) = Action.async {
    val artist = Artist(None, None, artistName, None, None, artistFacebookUrl)
    searchYoutubeTracks.getYoutubeTracksByArtistAndTitle(artist, trackTitle) map { tracks =>
      val trackTitleResearched = removeSpecialCharacters(trackTitle)
      val tracksCorrespondingToTheResearch = tracks.filterNot(track =>
        removeSpecialCharacters(track.title).equalsIgnoreCase(trackTitleResearched))

      val tracksFiltered = trackMethods.removeDuplicateByTitleAndArtistName(tracksCorrespondingToTheResearch)

      Future(trackMethods.saveSequence(tracksFiltered.toSet))

      Ok(Json.toJson(tracksFiltered))
    }
  }

  def getYoutubeTrackInfo(youtubeId: String) = Action.async {
    WS.url("http://www.youtube.com/get_video_info")
      .withQueryString(
        "video_id" -> youtubeId,
        "access_token" -> googleKey)
      .get()
      .map { youtubeWSResponse => Ok(youtubeWSResponse.body) }
  }
}
