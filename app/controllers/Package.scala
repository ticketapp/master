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

case class PlaylistUpdateTrackWithoutRank(message: String) extends Exception(message) {
  println("PlaylistUpdateTrackWithoutRank: " + message)
}
