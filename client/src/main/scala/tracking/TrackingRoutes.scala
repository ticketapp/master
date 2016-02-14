package tracking

trait TrackingRoutes {
  def postSession(screenWidth: Int, screenHeight: Int): String =
    "/sessions?screenWidth=" + screenWidth + "&screenHeight=" + screenHeight

  def getSessions: String = "/sessions"

  def postWithActionObject: String = "/actions"

  def getActionsBySessionId(sessionId: String): String = "/actions?sessionId=" + sessionId
}