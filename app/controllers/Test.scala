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

  def test1 = Action {
    //val yo = Enumerator.generateM[JsValue](Future { Option(Json.toJson("yo\n")) })
    val yo = Enumerator("345", "123", "476", "187687")
    //val tcho = Enumerator("tcho")

    //val yoTcho = yo.andThen(tcho)
    Ok.chunked(yo)
  }
}
