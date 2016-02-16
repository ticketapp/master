package tracking

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Session(uuid: String, ip: String, screenWidth: Int, screenHeight: Int)

case class Action(action: String, timestamp: Double, sessionId: String)