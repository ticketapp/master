package player


object PlayerRoutes {
  def findByArtist(artistFacebookUrl: String, numberToReturn: Int, offset: Int): String =
    "/tracks?artistFacebookUrl=" + artistFacebookUrl + "&numberToReturn=" + numberToReturn + "&offset=" + offset

  def getYoutubeTracksForArtistAndTrackTitle(artistName: String, artistFacebookUrl: String, trackTitle: String): String =
    "/tracks/"+ artistName +"/" +artistFacebookUrl+"/"+trackTitle

  def getYoutubeTrackInfo(youtubeId: String): String = "/tracks/youtubeTrackInfo/" + youtubeId

  def upsertRatingForUser(): String = "/rating"
}
