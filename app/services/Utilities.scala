package services

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
  def testIfExist(fieldName: String, value: Any): Boolean = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """SELECT exists(SELECT 1 FROM events where {fieldName}={checker} LIMIT 1)""")
          .on(
            'fieldName -> fieldName,
            'checker -> value)
          .execute()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot select in database with method Utilities.testIfExistById: " +
        e.getMessage)
    }
  }
}
