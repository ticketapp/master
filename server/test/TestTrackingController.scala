import java.sql.Timestamp
import java.util.{TimeZone, UUID}

import database.MyPostgresDriver.api._
import json.JsonHelper
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import trackingDomain.{UserAction, UserSession}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestTrackingController extends GlobalApplicationForControllers {
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
          VALUES(100, 'mm,30,30', TIMESTAMP '1970-01-01 00:00:00.005', 'a4cea509-1002-47d0-b55c-593c91cb32ae');

        INSERT INTO userActions(id, action, timestamp, sessionId)
          VALUES(1002, 'mm,30,30', current_timestamp, 'a4bea509-1002-47d0-b55c-593c91cb32ae');
        """),
      5.seconds)
  }

  TimeZone.setDefault(TimeZone.getTimeZone("UTC")) //For the equality between timestamp without timezone

  val savedIp = "127.0.0.0"
  val savedSession = UserSession(UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"), savedIp, 950, 450)
  val savedAction = UserAction("mm,30,30", new Timestamp(5), UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"))
  val savedCurrentSession = UserSession(UUID.fromString("a4bea509-1002-47d0-b55c-593c91cb32ae"), savedIp, 950, 450)

  "Tracking controller" should {

    "find sessions" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.findSessions()))
      val validatedJsonSalableEvents: JsResult[Seq[UserSession]] =
        contentAsJson(info).validate[Seq[UserSession]](JsonHelper.readUserSessionReads)

      val expectedSession = validatedJsonSalableEvents match {
        case sessions: JsSuccess[Seq[UserSession]] =>
          sessions.get
        case error: JsError =>
          throw new Exception
      }

      expectedSession must contain (savedSession)
    }

    "find current sessions" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.findCurrentSessions()))
      val validatedJsonSalableEvents: JsResult[Seq[UserSession]] =
        contentAsJson(info).validate[Seq[UserSession]](JsonHelper.readUserSessionReads)

      val expectedSession = validatedJsonSalableEvents match {
        case sessions: JsSuccess[Seq[UserSession]] =>
          sessions.get
        case error: JsError =>
          throw new Exception
      }

      expectedSession must contain (savedCurrentSession)
      expectedSession must not contain savedSession
    }

    "find actions by session id" in {
      val Some(info) = route(FakeRequest(
        trackingDomain.routes.TrackingController.findActionsBySessionId(savedSession.uuid.toString)))
      val validatedJsonSalableEvents: JsResult[Seq[UserAction]] =
        contentAsJson(info).validate[Seq[UserAction]](JsonHelper.readUserActionReads)

      val expectedAction = validatedJsonSalableEvents match {
        case actions: JsSuccess[Seq[UserAction]] => actions.get
        case error: JsError => throw new Exception
      }

      expectedAction must contain (savedAction)
    }

    "save a session" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.saveUserSession(950, 450)))

      val stringUUID = contentAsJson(info).asInstanceOf[JsString].value.toString

      stringUUID.length must be > 10
    }

    "save an action" in {
      val jsonAction = Json.parse(
        """{
          "action": "mm,300,300",
          "timestamp": 8,
          "sessionId": "a4cea509-1002-47d0-b55c-593c91cb32ae"
          }"""
      )

      val Some(info) = route(FakeRequest(
        trackingDomain.routes.TrackingController.saveUserAction()).withJsonBody(jsonAction))

      contentAsString(info).toInt mustEqual 1
    }
  }
}