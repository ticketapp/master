import java.sql.Timestamp
import java.util.UUID

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.Span
import testsHelper.GlobalApplicationForModels
import ticketsDomain._
import trackingDomain.{UserAction, UserSession}

import scala.language.postfixOps

class TestTrackingMethods extends GlobalApplicationForModels {

  val savedIp = "127.0.0.0"
  val savedSession = UserSession(UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"), savedIp)
  val savedAction = UserAction("mm,30,30", new Timestamp(5), UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"))

  "A tracking method" must {
      "save a new user session" in {
        val session = new UserSession(UUID.randomUUID(), savedIp)
        whenReady(trackingMethods.saveUserSession(session)) { response =>
          response mustBe 1
        }
      }

      "find all user sessions" in {
        whenReady(trackingMethods.findUserSessions) { response =>
          response must contain(savedSession)
        }
      }

    "save a user action" in {
      val userAction = UserAction("mm,20,20", new Timestamp(1), savedSession.id)
      whenReady(trackingMethods.saveUserAction(userAction)) { response =>
        response must be(1)
      }
    }

    "find all user actions for a session" in {
      whenReady(trackingMethods.findUserActionBySessionId(savedSession.id)) { response =>
        val date = response.head.timestamp
        response must contain(savedAction.copy(timestamp = date))
      }
    }
  }

}