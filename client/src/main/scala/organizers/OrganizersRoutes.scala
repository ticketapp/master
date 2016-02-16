package organizers

object OrganizersRoutes {
  def findAllSinceOffset(offset: Long, numberToReturn: Long): String =
    "/organizers?offset=" + offset + "&numberToReturn=" + numberToReturn

  def findById(id: Long): String = "/organizers/" + id

  def followByOrganizerId(organizerId: Long): String = "organizers/" + organizerId + "/followByOrganizerId"

  def unfollowByOrganizerId(organizerId: Long): String = "organizers/" + organizerId + "/unfollowOrganizerByOrganizerId"

  def followByFacebookId(facebookId: String): String = "/organizers/" + facebookId + "/followByFacebookId"

  def isFollowed(organizerId: Long): String = "/organizers/" + organizerId + "/isFollowed "

  def findContaining(pattern: String): String = "/organizers/containing/" + pattern

  def create: String = "/organizers/create"

  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/organizers/findNearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset

  def findNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
    "/organizers/findNearGeoPoint/?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn +
      "&offset=" + offset

  def getFollowed: String = "/organizers/followed/"
}