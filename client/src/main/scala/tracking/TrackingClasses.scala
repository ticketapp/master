package tracking

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Session(id: Int, ip: String)
case class Action(action: String, timestamp: Double, sessionId: Int)