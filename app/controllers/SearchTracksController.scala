package controllers

import play.api.mvc.Controller
import models.{Artist, Track}
import json.JsonHelper._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.SearchYoutubeTracks._

object SearchTracksController extends Controller {
  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String) =
  Action.async {
    getYoutubeTracksByTitleAndArtistName(Artist(None, None, artistName, None, None, artistFacebookUrl), trackTitle)
      .map { tracks => Ok(Json.toJson(tracks.filterNot(_.title contains trackTitle))) }
  }
}
