package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import java.util.regex.Pattern
import play.api.libs.iteratee.{Concurrent, Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.libs.F.Promise
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Track, Artist, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._
import play.api.libs.functional.syntax._

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
      val ps = Seq("0650415125", "04 75 64 30 65", "07.50.89.63.36", "0475643065, 0750896336",
      "0033 75 64 30 65-0610646630")

    def normalizeNumber(phoneNumbers: Seq[String]): Seq[Seq[String]] = {
      println(phoneNumbers)
      phoneNumbers.map { phoneNumber => phoneNumber.replaceAll("[^0-9,]", "").split(",").toSeq
        .map { phoneNumber =>
        //        if (phoneNumber.length > 12) {
        println(phoneNumber.drop(12))
        println(phoneNumber.drop(phoneNumber.indexOf('0', 10)))
        phoneNumber
      }
      }
    }
    println(normalizeNumber(ps))

    val a = Seq("0231862915", "04 72 76 89 09", "00336504154", "033746464",
      "33654654546", "01.40.20.40.25 (Billetterie)", "+33299673212")
    for (b <- a) {
      if (b.replaceAll("[^0-9,]", "").startsWith("0033")) {
        println("0" + b.replaceAll("[^0-9,]", "").drop(4))
      }
      if (b.replaceAll("[^0-9,]", "").startsWith("33")) {
        println("0" + b.replaceAll("[^0-9,]", "").drop(4))
      }
    }

    Ok
  }
}
