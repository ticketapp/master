import java.sql.Timestamp
import java.util.UUID

import database.MyPostgresDriver.api._
import org.scalatest.concurrent.ScalaFutures._
import testsHelper.GlobalApplicationForModelsIntegration
import trackingDomain.{UserAction, UserSession}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TrackingMethodsIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO guestUsers(ip) VALUES('127.0.0.0');

        INSERT INTO userSessions(id, ip, screenWidth, screenHeight)
          VALUES('a4cea509-1002-47d0-b55c-593c91cb32ae', '127.0.0.0', 950, 450);

        INSERT INTO userSessions(id, ip, screenWidth, screenHeight)
          VALUES('a4bea509-1002-47d0-b55c-593c91cb32ae', '127.0.0.0', 950, 450);

        INSERT INTO userActions(id, action, timestamp, sessionId)
          VALUES(100, 'mm,30,30', timestamp '1970-01-01 01:00:00.005', 'a4cea509-1002-47d0-b55c-593c91cb32ae');

        INSERT INTO userActions(id, action, timestamp, sessionId)
          VALUES(1002, 'mm,30,30', current_timestamp, 'a4bea509-1002-47d0-b55c-593c91cb32ae');
        """),
      5.seconds)
  }

  val savedIp = "127.0.0.0"
  val savedSession = UserSession(UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"), savedIp, 950, 450)
  val savedCurrentSession = UserSession(UUID.fromString("a4bea509-1002-47d0-b55c-593c91cb32ae"), savedIp, 950, 450)
  val savedAction = UserAction("mm,30,30", new Timestamp(5), UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"))

  "A tracking method" must {

    "save a new user session" in {
      val session = new UserSession(UUID.randomUUID(), savedIp, 950, 450)
      whenReady(trackingMethods.saveUserSession(session)) { response =>
        response mustBe 1
      }
    }

    "find all user sessions" in {
      whenReady(trackingMethods.findUserSessions) { response =>
        response must contain(savedSession)
      }
    }

    "find all current sessions" in {
      whenReady(trackingMethods.findInProgressSession) { response =>
        response must contain(savedCurrentSession)
        response must not contain(savedSession)
      }
    }

    "save a user action" in {
      val userAction = UserAction("mm,20,20", new Timestamp(1), savedSession.uuid)
      whenReady(trackingMethods.saveUserAction(userAction)) { response =>
        response must be(1)
      }
    }

    "find all user actions for a session" in {
      whenReady(trackingMethods.findUserActionBySessionId(savedSession.uuid)) { response =>
        val date = response.head.timestamp
        response must contain(savedAction.copy(timestamp = date))
      }
    }
  }

}