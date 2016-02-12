import java.sql.Timestamp
import java.util.UUID

import json.JsonHelper
import play.api.Logger
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import trackingDomain.{UserAction, UserSession}

import scala.language.postfixOps

class TestTrackingController extends GlobalApplicationForControllers {
  sequential

  val savedIp = "127.0.0.0"
  val savedSession = UserSession(UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"), savedIp, 950, 450)
  val savedAction = UserAction("mm,30,30", new Timestamp(5), UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"))

  "tracking controller" should {

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

    "find actions by session id" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.findActionsBySessionId(savedSession.uuid.toString)))
      val validatedJsonSalableEvents: JsResult[Seq[UserAction]] =
        contentAsJson(info).validate[Seq[UserAction]](JsonHelper.readUserActionReads)

      val expectedAction = validatedJsonSalableEvents match {
        case actions: JsSuccess[Seq[UserAction]] =>
          actions.get
        case error: JsError =>
          throw new Exception
      }

      expectedAction must contain (savedAction)
    }

    "save a session" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.saveUserSession(950, 450)))

      val stringUUID = contentAsJson(info).asInstanceOf[JsString].value.toString
      val uuid = UUID.fromString(stringUUID)

      stringUUID.length must be > 10
    }

    "save an action" in {
      val jsonAction =Json.parse(
        """{
          "action": "mm,300,300",
          "timestamp": 8,
          "sessionId": "a4cea509-1002-47d0-b55c-593c91cb32ae"
          }"""
      )

      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.saveUserAction()).withJsonBody(jsonAction))

      contentAsString(info).toInt mustEqual 1
    }
  }
}