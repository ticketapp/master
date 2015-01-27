package controllers

import play.api.libs.ws.WS
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._

object Teeest extends Controller{
  def teest = Action {
    /*val futureResult: Future[String] = WS.url("http://localhost:9000/artists").get() {
      response =>
        println(response)
    }*/

    Ok("qeqdqsd")
  }
}
