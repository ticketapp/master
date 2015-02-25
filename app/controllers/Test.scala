package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import play.api.libs.iteratee.{Iteratee, Enumerator}
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

object Test extends Controller{

  /*def test1 = WebSocket.using[String] { request =>
    // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnecteddddd")
    }

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!")

    (in, out)
  }*/

  def mock(serviceName: String) = {
    val start = System.currentTimeMillis()
    def getLatency(r: Any): Long = System.currentTimeMillis() - start
    val token = play.Play.application.configuration.getString("facebook.token")
    val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
    val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
    val youtubeKey = play.Play.application.configuration.getString("youtube.key")
    serviceName match {
      case "a" =>
        WS.url("http://api.soundcloud.com/users/rone-music?client_id=" +
          soundCloudClientId).get().map { response => Json.toJson("yo")}
      case "b" =>
        WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
          + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
          .map { response => Json.toJson("sacoche!!!")}
      case _ => WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
        + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
        .map { response => Json.toJson("???!!!")}
    }
  }


  def test1 = Action {
    //val yo = Enumerator.generateM[JsValue](Future { Option(Json.toJson("yo\n")) })
    val yo = Enumerator(Seq("a", "b", "c", "d"))
    //val tcho = Enumerator("tcho")

    //val yoTcho = yo.andThen(tcho)

    val a = Enumerator.flatten(mock("a").map { str => Enumerator(Json.toJson(Map("champ" -> str))) })
    val b = Enumerator.flatten(mock("b").map { str => Enumerator(Json.toJson(Map("champ" -> str))) })


    val body = Enumerator.interleave(b, a)

    Ok.chunked(body)
  }
}
