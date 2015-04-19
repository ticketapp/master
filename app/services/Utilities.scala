package services

import java.sql.Connection
import java.text.Normalizer

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

import scala.collection.mutable.ListBuffer

object Utilities {
  implicit def geographicPointToString: Column[String] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: Any => Right(d.toString)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass +
        " to Float for column " + qualified) )
    }
  }

  implicit def columnToChar: Column[Char] = Column[Char](transformer = { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case ch: String => Right(ch.head)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + " to Char for column " + qualified))
    }
  })


  def normalizeString(string: String): String =
    Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\x28-\\x5A\\x61-\\x7A]", "")

  def stripChars(s:String, ch:String)= s filterNot (ch contains _)

  def normalizeUrl(website: String): String =
    """(https?:\/\/(www\.)?)|(www\.)""".r.replaceAllIn(website.toLowerCase, p => "").stripSuffix("/")

  def testIfExist(table: String, fieldName: String, valueAnyType: Any)(implicit connection: Connection): Boolean = {
    val value = valueAnyType match {
      case Some(v: Int) => v
      case Some(v: String) => v
      case v: Int => v
      case v: String => v
      case _ => None
    }
    try {
      DB.withConnection { implicit connection =>
        SQL(s"""SELECT exists(SELECT 1 FROM $table where $fieldName={value} LIMIT 1)""")
          .on("value" -> value)
          .as(scalar[Boolean].single)
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot select in database with method Utilities.testIfExistById: " +
        e.getMessage)
    }
  }

  def getNormalizedWebsitesInText(maybeDescription: Option[String]): Set[String] = maybeDescription match {
    case None =>
      Set.empty
    case Some(description) =>
      play.Play.application.configuration.getString("regex.linkPattern").r
        .findAllIn(description).toSet.map { normalizeUrl }
  }

  def phoneNumbersStringToSet(phoneNumbers: Option[String]): Set[String] = phoneNumbers match {
    case None => Set.empty
    case Some(phoneNumbersValue: String) =>
      def normalizePhoneNumberPrefix(phoneNumber: String): String = phoneNumber match {
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("0033") =>
          "0" + phoneNumber.drop(4)
        case phoneNumberStartsWith0033 if phoneNumberStartsWith0033.startsWith("+0033") =>
          "0" + phoneNumber.drop(5)
        case phoneNumberStartsWith33 if phoneNumberStartsWith33.startsWith("33") =>
          "0" + phoneNumber.drop(2)
        case phoneNumberStartsWithPlus33 if phoneNumberStartsWithPlus33.startsWith("+33") =>
          "0" + phoneNumber.drop(3)
        case alreadyNormalized: String => alreadyNormalized
      }

      var numberWithoutLetters = phoneNumbersValue.replaceAll("[^0-9+]", "")
      var normalizedNumbers = ListBuffer.empty[String]

      while (numberWithoutLetters.length >= 10) {
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      normalizedNumbers.toSet
  }
}
