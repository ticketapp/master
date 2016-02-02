package artists

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
object ArtistsRoutes {
  def find(numberToReturn: Int, offset: Int): String =
    "/artists/since?offset=" + offset + "&numberToReturn=" + numberToReturn
  
  def FindByFacebookUrl(facebookUrl: String): String =
    "/artists/" + facebookUrl
  
  def findAllByArtist(facebookUrl: String, numberToReturn: Int, offset: Int): String =
    "/artists/" + facebookUrl + "/tracks"
  
  def find(id: Int): String =
    "/artists/byId/" + id
  
  def create: String =
    "/artists/createArtist"
  
  def followByArtistId(artistId: Long): String =
    "artists/" + artistId + "/followByArtistId"
  
  def unfollowByArtistId(artistId: Long): String =
    "artists/" + artistId + "/unfollowArtistByArtistId"
  
  def followByFacebookId(facebookId: String): String =
    "artists/" + facebookId + "/followByFacebookId"
  
  def getFollowed: String =
    "/artists/followed/"
  
  def isFollowed(artistId: Long): String =
    "/artists/" + artistId + "/isFollowed"
  
  def findContaining(pattern: String): String =
    "/artists/containing/" + pattern
  
  def getFacebookArtistsContaining(pattern: String): String =
    "/artists/facebookContaining/" + pattern
}