package controllers

import play.api.libs.ws.WS
import play.api.mvc._

object Teeest extends Controller{
  def teest = Action {
    var returned = WS.url("http://localhost:9000/artists").get()
    println(returned)
    Ok("qeqdqsd")
  }
}
