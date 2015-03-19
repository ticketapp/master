package controllers

case class DAOException(message: String) extends Exception(message) {
  println("DAO " + message)
}

case class WebServiceException(message: String) extends Exception(message) {
  println("WS " + message)
}
