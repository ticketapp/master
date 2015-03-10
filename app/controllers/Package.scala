package controllers

case class DAOException(message: String) extends Exception(message) {
  println(message)
}

case class WebServiceException(message: String) extends Exception(message) {
  println(message)
}
