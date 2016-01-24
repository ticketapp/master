package Organizers

import scala.scalajs.js

@js.native
object OrganizersRoutes {
  def findAllSinceOffset(offset: Long, numberToReturn: Long): String =
    "/organizers?offset=" + offset + "&numberToReturn=" + numberToReturn
  def findById(id: Long): String =
      "/organizers/" + id
  def followOrganizerByOrganizerId(organizerId: Long): String =
      "organizers/" + organizerId + "/followByOrganizerId"
  def unfollowOrganizerByOrganizerId(organizerId: Long): String =
      "organizers/" + organizerId + "/unfollowOrganizerByOrganizerId"
  def followOrganizerByFacebookId(facebookId: String): String =
      "/organizers/" + facebookId + "/followByFacebookId"
  def isOrganizerFollowed(organizerId: Long): String =
      "/organizers/" + organizerId + "/isFollowed "
  def findOrganizersContaining(pattern: String): String =
      "/organizers/containing/" + pattern
  def createOrganizer: String =
      "/organizers/create"
  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
      "/organizers/findNearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset
  def findOrganizersNear(geographicPoint: String, numberToReturn: Int, offset: Int): String =
      "/organizers/findNearGeoPoint/?geographicPoint=" + geographicPoint + "&numberToReturn=" + numberToReturn + "&offset=" + offset
  def getFollowedOrganizers: String =
      "/organizers/followed/"

}