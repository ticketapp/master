package Places

import scala.scalajs.js

@js.native
object PlacesRoutes {
  def findAllSinceOffset(offset: Long, numberToReturn: Long): String =
    "/places?offset=" + offset + "&numberToReturn=" + numberToReturn
  def findById(id: Long): String =
    "/places/" + id
  def followPlaceByPlaceId(placeId: Long): String =
    "places/" + placeId + "/followByPlaceId"
  def unfollowPlaceByPlaceId(placeId: Long): String =
    "places/" + placeId + "/unfollowPlaceByPlaceId"
  def followPlaceByFacebookId(facebookId: String): String =
    "/places/" + facebookId + "/followByFacebookId"
  def isPlaceFollowed(placeId: Long): String =
    "/places/" + placeId + "/isFollowed "
  def findPlacesContaining(pattern: String): String =
    "/places/containing/" + pattern
  def createPlace: String =
    "/places/create"
  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/places/nearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset
  def findPlacesNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/places?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn + "&offset=" + offset
  def getFollowedPlaces: String =
    "/places/followed/"

}
