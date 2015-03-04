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
import services.Utilities.{ normalizeUrl, normalizeString }
import services.SearchSoundCloudTracks._
import services.SearchYoutubeTracks._
import controllers.SearchArtistsController._

object SearchTracksController extends Controller {
  def getTracksForArtist(pattern: String) = Action.async {
    get20FacebookArtists(pattern).map { _.map {
      val futureYoutubeTracks =
        getYoutubeTracksForArtist(facebookArtist, pattern)
    }
      val youtubeTracksEnumerator = Enumerator.flatten(
        futureYoutubeTracks.map { youtubeTracks =>
          Enumerator(Json.toJson(youtubeTracks))
        }
      )
    }

    val artists = get20FacebookArtists(pattern).map { artists =>
      artists.map { a => getSoundCloudTracksForArtist(a) }
    }

      futureSoundCloudTracks.flatMap { soundCloudTracks =>
        Ok(Json.toJson(soundCloudTracks))
      }

        /*val soundCloudTracksEnumerator = Enumerator.flatten(
          futureSoundCloudTracks.map { soundCloudTracks =>
            Enumerator( Json.toJson(soundCloudTracks) )
          }
        )*/


      /*

      val futureYoutubeTracks =
          getYoutubeTracksForArtist(facebookArtist, pattern)

      val youtubeTracksEnumerator = Enumerator.flatten(
        futureYoutubeTracks.map { youtubeTracks =>
          Enumerator( Json.toJson(youtubeTracks) )
        }
      )

      val enumerators = Enumerator.interleave(soundCloudTracksEnumerator, youtubeTracksEnumerator)
      Ok.chunked(enumerators)*/

    }
  }
}
