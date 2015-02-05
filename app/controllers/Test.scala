package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
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

object Test extends Controller{

  def test1 = Action { request =>
    /*WS.url("http://ip-api.com/json/" + request.remoteAddress).get.map( response => {
      println(response.json)
    } )*/

    val token = play.Play.application.configuration.getString("facebook.token")

    val eventFuture = WS.url("https://graph.facebook.com/v2.2/ez.dubstep.night&access_token=" + token).get
    eventFuture onComplete {
      case Success(posts) => println(posts)
      case Failure(t) => println("An error has occured: " + t.getMessage)
    }
    println("avant ??")


    Ok("qsdqsdqs")
  }
}
