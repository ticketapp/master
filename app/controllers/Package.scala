package controllers

import play.api.Logger

case class DAOException(message: String) extends Exception(message) {
  Logger.error("DAOException: " + message)
}

case class WebServiceException(message: String) extends Exception(message) {
  Logger.error("WebServiceException: " + message)
}

case class SchedulerException(message: String) extends Exception(message) {
  Logger.error("SchedulerException: " + message)
}

case class PlaylistDoesNotExistException(message: String) extends Exception(message) {
  Logger.error("PlaylistDoesNotExistException: " + message)
}

case class PlaylistUpdateTrackWithoutRankException(message: String) extends Exception(message) {
  Logger.error("PlaylistUpdateTrackWithoutRankException: " + message)
}

case class ArtistContainsEmptyWebsiteException(message: String) extends Exception(message) {
  Logger.error("ArtistContainsEmptyWebsiteException: " + message)
}

case class UserOAuth2InfoWronglyFormatted(message: String) extends Exception(message) {
  Logger.error("UserOAuth2InfoWronglyFormatted: " + message)
}

case class ThereIsNoArtistForThisFacebookIdException(message: String) extends Exception(message) {
  Logger.error("ThereIsNoArtistForThisFacebookIdException: " + message)
}

case class ThereIsNoPlaceForThisFacebookIdException(message: String) extends Exception(message) {
  Logger.error("ThereIsNoPlaceForThisFacebookIdException: " + message)
}

case class ThereIsNoOrganizerForThisFacebookIdException(message: String) extends Exception(message) {
  Logger.error("ThereIsNoOrganizerForThisFacebookIdException: " + message)
}
