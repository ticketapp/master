package tracking

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Session(id: String, ip: String)
case class Action(action: String, timestamp: Double, sessionId: String)