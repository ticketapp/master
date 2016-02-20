package artists

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService
import materialDesign.{MdToastService, MdToast}
import upickle.Invalid.Json
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import upickle.default._
import org.scalajs.dom.console

@JSExportAll
@injectable("artistsController")
class ArtistsController(scope: Scope, service: HttpGeneralService, timeout: Timeout, mdToast: MdToastService)
  extends AbstractController[Scope](scope) with jsonHelper {
  var artists: js.Any = Nil

  def findById(id: Int): Unit = {
    service.get(ArtistsRoutes.find(id)) map { foundArtist =>
      timeout(() => artists = js.Array(read[ArtistWithWeightedGenres](foundArtist)))
    }
  }

  def findByFacebookUrl(facebookUrl: String): Unit = {
    service.get(ArtistsRoutes.findByFacebookUrl(facebookUrl)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def find(numberToReturn: Int, offset: Int): Unit = {
    service.get(ArtistsRoutes.find(numberToReturn: Int, offset: Int)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def getFollowed(): Unit = {
    service.get(ArtistsRoutes.getFollowed) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def getFacebookContaining(pattern: String): Unit = {
    service.get(ArtistsRoutes.getFacebookArtistsContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def findContaining(pattern: String): Unit = {
    service.get(ArtistsRoutes.findContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = JSON.parse(foundArtist))
    }
  }

  def update(artist: js.Any): Unit = {
    service.updateWithObject(ArtistsRoutes.update, artist) map { response =>
      val toast = mdToast.simple("artist update ok")
      mdToast.show(toast)
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