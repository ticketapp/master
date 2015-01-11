package controllers

case class DAOException(message: String) extends  Exception(message)
