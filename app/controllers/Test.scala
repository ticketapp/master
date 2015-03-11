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
    Ok(Json.parse("""{"results":[{"address_components":[{"long_name":"3","short_name":"3","types":["street_number"]},{"long_name":"Boulevard de Stalingrad","short_name":"Boulevard de Stalingrad","types":["route"]},{"long_name":"Villeurbanne","short_name":"Villeurbanne","types":["locality","political"]},{"long_name":"Rhône","short_name":"69","types":["administrative_area_level_2","political"]},{"long_name":"Rhône-Alpes","short_name":"RA","types":["administrative_area_level_1","political"]},{"long_name":"France","short_name":"FR","types":["country","political"]},{"long_name":"69100","short_name":"69100","types":["postal_code"]}],"formatted_address":"3 Boulevard de Stalingrad, 69100 Villeurbanne, France","geometry":{"location":{"lat":45.7839103,"lng":4.860398399999999},"location_type":"ROOFTOP","viewport":{"northeast":{"lat":45.7852592802915,"lng":4.861747380291502},"southwest":{"lat":45.7825613197085,"lng":4.859049419708497}}},"partial_match":true,"place_id":"ChIJSdYF0r3q9EcRubfkg7mivfA","types":["street_address"]}],"status":"OK"} """))
  }
}
