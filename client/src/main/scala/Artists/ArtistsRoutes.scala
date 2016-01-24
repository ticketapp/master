package Artists

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
object ArtistsRoutes {
  def artistsSinceOffset(numberToReturn: Int, offset: Long): String =
    "/artists/since?offset=" + offset + "&numberToReturn=" + numberToReturn
  def artistByFacebookUrl(facebookUrl: String): String =
    "/artists/" + facebookUrl
  def findAllByArtist(facebookUrl: String, numberToReturn: Int, offset: Int): String =
    "/artists/" + facebookUrl + "/tracks"
  def artist(id: Long): String =
    "/artists/byId/" + id
  def createArtist: String =
    "/artists/createArtist"
  def followArtistByArtistId(artistId: Long): String =
    "artists/" + artistId + "/followByArtistId"
  def unfollowArtistByArtistId(artistId: Long): String =
    "artists/" + artistId + "/unfollowArtistByArtistId"
  def followArtistByFacebookId(facebookId: String): String =
    "artists/" + facebookId + "/followByFacebookId"
  def getFollowedArtists: String =
    "/artists/followed/"
  def isArtistFollowed(artistId: Long): String =
    "/artists/" + artistId + "/isFollowed"
  def findArtistsContaining(pattern: String): String =
    "/artists/containing/" + pattern
  def getFacebookArtistsContaining(pattern: String): String =
    "/artists/facebookContaining/" + pattern
}