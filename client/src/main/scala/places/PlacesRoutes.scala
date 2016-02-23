package places

object PlacesRoutes {

  def findById(id: Long): String = "/places/" + id
  
  def followByPlaceId(placeId: Long): String = "places/" + placeId + "/followByPlaceId"
  
  def unfollowByPlaceId(placeId: Long): String = "places/" + placeId + "/unfollowPlaceByPlaceId"
  
  def followByFacebookId(facebookId: String): String = "/places/" + facebookId + "/followByFacebookId"
  
  def isFollowed(placeId: Long): String = "/places/" + placeId + "/isFollowed "
  
  def findContaining(pattern: String): String = "/places/containing/" + pattern
  
  def create: String = "/places/create"
  
  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/places/nearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/places?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def getFollowed: String = "/places/followed/"

  def deleteEventRelation(eventId: Int, placeId: Int): String =
    "/eventPlace?eventId=" + eventId + "&placeId=" + placeId

  def saveEventRelation(eventId: Int, placeId: Int): String =
    "/eventPlace?eventId=" + eventId + "&placeId=" + placeId
}
