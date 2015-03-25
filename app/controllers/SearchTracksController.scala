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
import services.Utilities.normalizeString
import scala.concurrent.Future
import play.api.libs.ws.Response

object SearchTracksController extends Controller {
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  //save tracks
  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String) =
  Action.async {
    getYoutubeTracksByTitleAndArtistName(Artist(None, None, artistName, None, None, artistFacebookUrl), trackTitle)
      .map { tracks =>
      Ok(Json.toJson(tracks.filter(track =>
        normalizeString(track.title).toLowerCase contains normalizeString(trackTitle).toLowerCase))
      )
    }
  }

  def getYoutubeTrackInfos(youtubeId: String) = Action.async {
    WS.url("http://www.youtube.com/get_video_info")
      .withQueryString(
        "video_id" -> youtubeId,
        "access_token" -> youtubeKey)
      .get()
      .map { youtubeResponse => Ok(youtubeResponse.body) }
  }
}
