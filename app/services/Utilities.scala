package services

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import models._
import play.api.db.DB
import securesocial.core.{OAuth2Info, OAuth1Info, PasswordInfo}
import play.api.libs.json.JsNumber
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play.current

object Utilities {
  def stripChars(s:String, ch:String)= s filterNot (ch contains _)

  def testIfExist(table: String, fieldName: String, valueAnyType: Any): Boolean = {
    val value = valueAnyType match {
      case Some(v: Int) => v
      case Some(v: String) => v
      case v: Int => v
      case v: String => v
      case _ => None
    }
    try {
      DB.withConnection { implicit connection =>
        SQL(s"""SELECT exists(SELECT 1 FROM $table where $fieldName={value} LIMIT 1)"""
        ).on(
            "value" -> value
          ).as(scalar[Boolean].single)//.singleOpt _)
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot select in database with method Utilities.testIfExistById: " +
        e.getMessage)
    }
  }
}
