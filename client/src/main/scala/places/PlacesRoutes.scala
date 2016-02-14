package places

trait PlacesRoutes {

  def findByIdRoute(id: Long): String = "/places/" + id
  
  def followByPlaceIdRoute(placeId: Long): String = "places/" + placeId + "/followByPlaceId"
  
  def unfollowByPlaceIdRoute(placeId: Long): String = "places/" + placeId + "/unfollowPlaceByPlaceId"
  
  def followByFacebookIdRoute(facebookId: String): String = "/places/" + facebookId + "/followByFacebookId"
  
  def isFollowedRoute(placeId: Long): String = "/places/" + placeId + "/isFollowed "
  
  def findContainingRoute(pattern: String): String = "/places/containing/" + pattern
  
  def createRoute: String = "/places/create"
  
  def findNearCityRoute(city: String, numberToReturn: Int, offset: Int): String =
    "/places/nearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def findNearRoute(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/places?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn + "&offset=" + offset
  
  def getFollowedRoute: String = "/places/followed/"
}
