package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.Event
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._

object Test extends Controller{
  //val token = play.Play.application.configuration.getString("facebook.token")
  def test1 = Action.async { request =>
    val token = play.Play.application.configuration.getString("facebook.token")
    val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
    val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
    val youtubeKey = play.Play.application.configuration.getString("youtube.key")

    val start = System.currentTimeMillis()
    def getLatency(r: Any): Long = System.currentTimeMillis() - start



    val a = WS.url("http://api.soundcloud.com/users/rone-music?client_id=" +
      soundCloudClientId).get().map{getLatency}

    val b = WS.url("http://developer.echonest.com/api/v4/artist/search?api_key=" + echonestApiKey + "&name=" +
    "asco" + "&format=json&bucket=urls&bucket=images&bucket=id:facebook" ).get()
    .map {getLatency}

    val c = WS.url("https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
    "asco" + "&type=video&videoCategoryId=10&key=" + youtubeKey ).get().map {getLatency}

    val d = WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
      + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
      .map {getLatency}

    Future.sequence(Seq(a, b, c, d)).map { case times =>
      Ok(Json.toJson(Map("SC" -> times(0), "EN" -> times(1), "YT" -> times(2), "FB" -> times(3))))
    }

   /* WS.url("http://ip-api.com/json/" + request.remoteAddress).get.map( response => {
      println(response.json)
    } )*/

    //Ok("Okay")
  }
}
