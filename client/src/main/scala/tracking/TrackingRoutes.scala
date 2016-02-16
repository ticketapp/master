package tracking

object TrackingRoutes {
  def postSession(screenWidth: Int, screenHeight: Int): String =
    "/sessions?screenWidth=" + screenWidth + "&screenHeight=" + screenHeight

  def getSessions: String = "/sessions"

  def getCurrentSessions: String = "/sessions/current"

  def postWithActionObject: String = "/actions"

  def getActionsBySessionId(sessionId: String): String = "/actions?sessionId=" + sessionId
}