package others

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import java.util.regex.Pattern

import play.api.libs.iteratee.{Concurrent, Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.libs.F.Promise
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import play.api.libs.functional.syntax._
import play.api.Play.current
/*
 follower counts SC
 regarder le next de facebook
 aller chercher les artistes d'une lettre et deux lettres
 enlever 0.1 par lettre en plus au temps de latence à partir de trois
 un seul match echonest : la prendre et lier à la page fb qi à le plus de followers (exemple ibeyi)
 hendrix : bug si getechoSongs recursif
 */


class Test extends Controller {
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


    Ok
  }
}
