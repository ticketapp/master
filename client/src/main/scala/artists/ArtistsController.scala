package artists

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("artistsController")
class ArtistsController(scope: Scope, httpGeneralService: HttpGeneralService, timeout: Timeout,
                        mdToastService: MdToastService)
  extends AbstractController[Scope](scope) with jsonHelper {
  var artists: js.Any = Nil

  def findById(id: Int): Unit = {
    httpGeneralService.get(ArtistsRoutes.find(id)) map { foundArtist =>
      timeout(() => artists = js.Array(read[ArtistWithWeightedGenres](foundArtist)))
    }
  }

  def findByFacebookUrl(facebookUrl: String): Unit = {
    httpGeneralService.get(ArtistsRoutes.findByFacebookUrl(facebookUrl)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def find(numberToReturn: Int, offset: Int): Unit = {
    httpGeneralService.get(ArtistsRoutes.find(numberToReturn: Int, offset: Int)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def getFollowed(): Unit = {
    httpGeneralService.get(ArtistsRoutes.getFollowed) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def getFacebookContaining(pattern: String): Unit = {
    httpGeneralService.get(ArtistsRoutes.getFacebookArtistsContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def findContaining(pattern: String): Unit = {
    httpGeneralService.get(ArtistsRoutes.findContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def update(artist: js.Any): Unit = {
    httpGeneralService.updateWithObject(ArtistsRoutes.update, artist) map { response =>
      val toast = mdToastService.simple("artist update ok")
      mdToastService.show(toast)
    }
  }
  
  def deleteEventRelation(eventId: Int, artistId: Int): Unit = {
    httpGeneralService.delete(ArtistsRoutes.deleteEventRelation(eventId, artistId)) map { result =>
      val toast = mdToastService.simple("relation deleted")
      mdToastService.show(toast)
    }
  }

  def saveEventRelation(eventId: Int, artistId: Int): Unit = {
    httpGeneralService.post(ArtistsRoutes.saveEventRelation(eventId, artistId)) map { result =>
      val toast = mdToastService.simple("relation added")
      mdToastService.show(toast)
    }
  }
}

/*
  def createArtist: String =
    "/artists/createArtist"
  def followArtistByArtistId(artistId: Long): String =
    "artists/" + artistId + "/followByArtistId"
  def unfollowArtistByArtistId(artistId: Long): String =
    "artists/" + artistId + "/unfollowArtistByArtistId"
  def followArtistByFacebookId(facebookId: String): String =
    "artists/" + facebookId + "/followByFacebookId"
  def isArtistFollowed(artistId: Long): String =
    "/artists/" + artistId + "/isFollowed"
*/