package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import play.api.libs.iteratee.{Concurrent, Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.libs.F.Promise
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.Event
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._

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
    Ok(Json.parse("""{"name":"MUST DIE","cover":{"source":"https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-xpt1/v/t1.0-9/s720x720/1604978_824844067586682_4560433178241458922_n.png?oh=5cd498b5178e4dccb0324c63a6c97bf1&oe=557A8B7D&__gda__=1438383963_fdc4f8f80ce12a85d54271457c634b9f","id":"824844067586682"},"id":"199716293432799","category":"Musician/band","link":"https://www.facebook.com/MUSTDIEmusic","website":"http://mustdiemusic.com/","description":"Watashi Wa MUST DIE! Desunnmustdiemusic.comnnProfessional Skull Kid. Kawaii as fuck. 1991.nn","genre":"Eternal Sorcery"}"""))
  }
}
