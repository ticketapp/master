package controllers

case class DAOException(message: String) extends Exception(message)

case class WebServiceException(message: String) extends Exception(message)
