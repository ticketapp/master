package controllers

import play.api.libs.ws.WS
import play.api.mvc.Controller
import models.{Artist, Track}
import json.JsonHelper._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.SearchYoutubeTracks._
import services.Utilities.{normalizeString, removeSpecialCharacters, googleKey}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import play.api.Play.current

class SearchTracksController extends Controller {

  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String) =
  Action.async {
    getYoutubeTracksByArtistAndTitle(Artist(None, None, artistName, None, None, artistFacebookUrl), trackTitle)
      .map { tracks =>
        val tracksFiltered = Track.removeDuplicateByTitleAndArtistName(tracks.filterNot(track =>
          removeSpecialCharacters(track.title).equalsIgnoreCase(removeSpecialCharacters(trackTitle))))

        Future { tracksFiltered map Track.save }
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
