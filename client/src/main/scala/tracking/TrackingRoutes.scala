package tracking

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object TrackingRoutes {
  def postSession: String = "/sessions"
  def getSessions: String = "/sessions"
  def postWithActionObject: String = "/actions"
  def getActionsBySessionId(sessionId: String): String = "/actions?sessionId=" + sessionId
}