package controllers

import javax.inject.Inject

import play.api.libs.ws.WS
import play.api.mvc.Controller
import models.{TrackMethods, Artist, Track}
import json.JsonHelper._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.{SearchYoutubeTracks, Utilities}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import play.api.Play.current


class SearchTracksController @Inject()(val utilities: Utilities,
                                        val trackMethods: TrackMethods,
                                        val searchYoutubeTracks: SearchYoutubeTracks) extends Controller {
  val googleKey = utilities.googleKey

  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String) =
  Action.async {
    searchYoutubeTracks.getYoutubeTracksByArtistAndTitle(Artist(None, None, artistName, None, None, artistFacebookUrl), trackTitle)
      .map { tracks =>
        val tracksFiltered = trackMethods.removeDuplicateByTitleAndArtistName(tracks.filterNot(track =>
          utilities.removeSpecialCharacters(track.title).equalsIgnoreCase(utilities.removeSpecialCharacters(trackTitle))))

        Future { tracksFiltered map trackMethods.save }
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
