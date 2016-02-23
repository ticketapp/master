package organizers

object OrganizersRoutes {
  def findAllSinceOffset(offset: Long, numberToReturn: Long): String =
    "/organizers?offset=" + offset + "&numberToReturn=" + numberToReturn

  def findById(id: Long): String = "/organizers/" + id

  def followByOrganizerId(organizerId: Long): String = "followedOrganizers/organizerId/" + organizerId

  def unfollowByOrganizerId(organizerId: Long): String = "followedOrganizers/organizerId/" + organizerId

  def followByFacebookId(facebookId: String): String = "followedOrganizers/facebookId/" + facebookId

  def isFollowed(organizerId: Long): String = "/followedOrganizers/" + organizerId

  def findContaining(pattern: String): String = "/organizers/containing/" + pattern

  def create: String = "/organizers/create"

  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/organizers/findNearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/organizers/findNearGeoPoint/?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn +
      "&offset=" + offset

  def getFollowed: String = "/followedOrganizers"
}