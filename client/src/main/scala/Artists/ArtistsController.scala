package Artists

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

  def findArtistById(id: Int): Unit = {
    service.getJson(ArtistsRoutes.artist(id)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }
  def findArtistByFacebookUrl(facebookUrl: String): Unit = {
    service.getJson(ArtistsRoutes.artistByFacebookUrl(facebookUrl)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }
  def artistsSinceOffset(numberToReturn: Int, offset: Long): Unit = {
    service.getJson(ArtistsRoutes.artistsSinceOffset(numberToReturn: Int, offset: Long)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }
  def getFollowedArtists: Unit = {
    service.getJson(ArtistsRoutes.getFollowedArtists) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }
  def getFacebookArtistsContaining(pattern: String): Unit = {
    service.getJson(ArtistsRoutes.getFacebookArtistsContaining(pattern: String)) map { foundArtist =>
      timeout(() => artists = js.Array(foundArtist))
    }
  }
  def findArtistsContaining(pattern: String): Unit = {
    service.getJson(ArtistsRoutes.findArtistsContaining(pattern: String)) map { foundArtist =>
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