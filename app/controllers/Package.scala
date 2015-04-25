package controllers

case class DAOException(message: String) extends Exception(message) {
  println("DAOException: " + message)
}

case class WebServiceException(message: String) extends Exception(message) {
  println("WebServiceException: " + message)
}

case class SchedulerException(message: String) extends Exception(message) {
  println("SchedulerException: " + message)
}

case class PlaylistDoesNotExistException(message: String) extends Exception(message) {
  println("PlaylistDoesNotExistException: " + message)
}

case class PlaylistUpdateTrackWithoutRankException(message: String) extends Exception(message) {
  println("PlaylistUpdateTrackWithoutRankException: " + message)
}

case class ArtistContainsEmptyWebsiteException(message: String) extends Exception(message) {
  println("ArtistContainsEmptyWebsiteException: " + message)
}

case class UserOAuth2InfoWronglyFormatted(message: String) extends Exception(message) {
  println("UserOAuth2InfoWronglyFormatted: " + message)
}

case class ThereIsNoArtistForThisFacebookIdException(message: String) extends Exception(message) {
  println("ThereIsNoArtistForThisFacebookIdException: " + message)
}
