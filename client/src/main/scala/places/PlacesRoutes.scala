package places

object PlacesRoutes {

  def findById(id: Int): String = "/places/" + id
  
  def followByPlaceId(placeId: Long): String = "/followedPlaces/placeId/" + placeId
  
  def unfollowByPlaceId(placeId: Long): String = "/followedPlaces/placeId/" + placeId
  
  def followByFacebookId(facebookId: String): String = "/followedPlaces/facebookId/" + facebookId
  
  def isFollowed(placeId: Long): String = "/followedPlaces/" + placeId
  
  def findContaining(pattern: String): String = "/places/containing/" + pattern
  
  def create: String = "/places"
  
  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/places/nearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/places?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def getFollowed: String = "/followedPlaces"

  def deleteEventRelation(eventId: Int, placeId: Int): String = "/eventPlace?eventId=" + eventId + "&placeId=" + placeId

  def saveEventRelation(eventId: Int, placeId: Int): String = "/eventPlace?eventId=" + eventId + "&placeId=" + placeId
}
