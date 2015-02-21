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
  def test1 = Action { request =>
    println(normalizeString("lkjl  kjlkjl%kjlkj\\l k*$ù{' m!"))

    WS.url("http://ip-api.com/json/" + request.remoteAddress).get.map( response => {
      println(response.json)
    } )

    Ok("Okay")
  }
}
