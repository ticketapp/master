package artists

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("artistsController")
class ArtistsController(scope: Scope, service: HttpGeneralService, timeout: Timeout)
  extends AbstractController[Scope](scope) {
  var artists: js.Array[String] = new js.Array[String]

  def findById(id: Int): Unit = {
    service.get(ArtistsRoutes.find(id)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }

  def findByFacebookUrl(facebookUrl: String): Unit = {
    service.get(ArtistsRoutes.FindByFacebookUrl(facebookUrl)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }

  def find(numberToReturn: Int, offset: Int): Unit = {
    service.get(ArtistsRoutes.find(numberToReturn: Int, offset: Int)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }

  def getFollowed: Unit = {
    service.get(ArtistsRoutes.getFollowed) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }

  def getFacebookContaining(pattern: String): Unit = {
    service.get(ArtistsRoutes.getFacebookArtistsContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }

  def findContaining(pattern: String): Unit = {
    service.get(ArtistsRoutes.findContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
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