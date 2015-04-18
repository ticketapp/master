package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import java.util.regex.Pattern
import anorm._
import play.api.db.DB
import play.api.libs.iteratee.{Concurrent, Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.libs.F.Promise
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Track, Artist, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._
import play.api.libs.functional.syntax._
import play.api.Play.current

object Test extends Controller {
  /*def test1 = WebSocket.using[String] { request =>
    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[String]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[String] {
      msg => println(msg)
        //the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
        //receive the pushed messages
        channel push("c'est toi le : " + msg)
    }
    (in,out)
  }*/

  def test1 = Action  {
//    val numbers = Seq("0231862915", "04 72 76 89 09", "00336504154", "033746464",
//      "33654654546", "01.40.20.40.25 (Billetterie)", "+33299673212")

    val numbers = Seq("+0033475640365 billeterie 0475630535 +0033457643065")

    var normalizedNumbers = ListBuffer.empty[String]

    for (number <- numbers) {
      var numberWithoutLetters = number.replaceAll("[^0-9+]", "")

      while (numberWithoutLetters.length >= 10) {
//        println(normalizePhoneNumberPrefix(numberWithoutLetters).take(10))
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      println(phoneNumbersStringToSet(numbers.head))
    }

    def phoneNumbersStringToSet(phoneNumbers: String): Set[String] = {
      var numberWithoutLetters = phoneNumbers.replaceAll("[^0-9+]", "")

      var normalizedNumbers = ListBuffer.empty[String]

      while (numberWithoutLetters.length >= 10) {
        val withNormalizedPrefix = normalizePhoneNumberPrefix(numberWithoutLetters)
        normalizedNumbers += withNormalizedPrefix.take(10)
        numberWithoutLetters = withNormalizedPrefix.drop(10)
      }
      normalizedNumbers.toSet
    }

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

    Ok
  }
}
