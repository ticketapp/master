package events

object EventsRoutes {
  def find(offset: Int, numberToReturn: Int, geographicPoint: String): String =
    "/events?offset=" + offset + "&numberToReturn=" + numberToReturn + "&geographicPoint=" + geographicPoint

  def findInHourInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): String =
    "/events/inInterval/" + hourInterval + "?offset=" + offset + "&numberToReturn=" + numberToReturn +
      "&geographicPoint=" + geographicPoint

  def findPassedInInterval(hourInterval: Int, geographicPoint: String, offset: Int, numberToReturn: Int): String =
    "/events/passedInInterval/" + hourInterval + "?offset=" + offset + "&numberToReturn=" + numberToReturn +
      "&geographicPoint=" + geographicPoint

  def findById(id: Long): String = "/events/" + id

  def create: String = "/events"

  def update: String = "/events"

  def createByFacebookId(facebookId: String): String = "/events/" + facebookId

  def follow(eventId: Long): String = "/followedEvents?eventId=" + eventId

  def unfollow(eventId: Long): String = "/followedEvents?eventId=" + eventId

  def getFollowed: String = "/followedEvents"

  def isFollowed(eventId: Long): String = "/events/" + eventId + "/isFollowed"

  def findAllContaining(pattern: String, geographicPoint: String): String =
    "/events/containing/" + pattern + "?geographicPoint=" + geographicPoint

  def findByCityPattern(pattern: String): String = "/events/city/" + pattern

  def findNearCity(city: String, numberToReturn: Int, offset: Int): String =
    "/events/nearCity/" + city + "?numberToReturn=" + numberToReturn + "&offset=" + offset

  def findByArtist(facebookUrl: String): String = "/artists/" + facebookUrl + "/events"

  def findPassedByArtist(artistId: Long): String = "/artists/" + artistId + "/passedEvents"

  def findByOrganizer(organizerId: Long): String = "/organizers/" + organizerId + "/events"

  def findPassedByOrganizer(organizerId: Long): String = "/organizers/" + organizerId + "/passedEvents"

  def findByPlace(placeId: Long): String = "/places/" + placeId + "/events"

  def findPassedByPlace(placeId: Long): String = "/places/" + placeId + "/passedEvents"

  def findMaybeSalableEvents(pattern: String): String = "/salableEvents/containing?pattern=" + pattern

  def findMaybeSalableEventsNear(geographicPoint: String, offset: Int, numberToReturn: Int): String =
    "/salableEventsNear?geographicPoint=" + geographicPoint + "&offset=" + offset + "&numberToReturn=" + numberToReturn
}
