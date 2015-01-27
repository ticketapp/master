package jobs

import play.api.libs.ws.WS

object Scheduler {
  def start = {
      println("yeah")
      /*var returned = WS.url("http://localhost:9000/artists").get()
      println(returned)*/
  }
}