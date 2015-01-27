package controllers


import play.api.libs.ws.WS
import play.api.mvc._


object facebookRobot extends Controller {
  var returned = WS.url("http://localhost:9000/artists").get()
  println(returned)
}
