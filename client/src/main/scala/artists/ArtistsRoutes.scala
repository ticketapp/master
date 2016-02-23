package artists

object ArtistsRoutes {
  def find(numberToReturn: Int, offset: Int): String =
    "/artists?offset=" + offset + "&numberToReturn=" + numberToReturn
  
  def findByFacebookUrl(facebookUrl: String): String = "/artists/" + facebookUrl
  
  def findAllByArtist(facebookUrl: String, numberToReturn: Int, offset: Int): String =
    "/artists/" + facebookUrl + "/tracks"
  
  def find(id: Int): String = "/artists/byId/" + id
  
  def create: String = "/artists"

  def update: String = "/artists"

  def followByArtistId(artistId: Long): String = "/followedArtists/artistId/" + artistId
  
  def unfollowByArtistId(artistId: Long): String = "/followedArtists/artistId/" + artistId
  
  def followByFacebookId(facebookId: String): String = "/followedArtists/facebookId/" + facebookId
  
  def getFollowed: String = "/followedArtists"
  
  def isFollowed(artistId: Long): String = "/followedArtists/" + artistId
  
  def findContaining(pattern: String): String = "/artists/containing/" + pattern
  
  def getFacebookArtistsContaining(pattern: String): String = "/artists/facebookContaining/" + pattern

  def deleteEventRelation(eventId: Int, artistId: Int): String =
    "/eventArtist?eventId=" + eventId + "&artistId=" + artistId

  def saveEventRelation(eventId: Int, artistId: Int): String =
    "/eventArtist?eventId=" + eventId + "&artistId=" + artistId
}