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
  val savedSession = UserSession(UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"), savedIp)
  val savedAction = UserAction("mm,30,30", new Timestamp(5), UUID.fromString("a4cea509-1002-47d0-b55c-593c91cb32ae"))

  "tracking controller" should {

    "find sessions" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.findSessions()))
      val validatedJsonSalableEvents: JsResult[Seq[UserSession]] =
        contentAsJson(info).validate[Seq[UserSession]](JsonHelper.readUserSessionReads)
      validatedJsonSalableEvents match {
        case sessions: JsSuccess[Seq[UserSession]] =>
          sessions.get must contain (savedSession)
        case error: JsError =>
          Logger.error("find sessions:" + error)
          error mustEqual 0
      }
    }

    "find actions by session id" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.findActionsBySessionId(savedSession.id.toString)))
      val validatedJsonSalableEvents: JsResult[Seq[UserAction]] =
        contentAsJson(info).validate[Seq[UserAction]](JsonHelper.readUserActionReads)
      validatedJsonSalableEvents match {
        case actions: JsSuccess[Seq[UserAction]] =>
          actions.get must contain (savedAction.copy(timestamp = actions.get.head.timestamp))
        case error: JsError =>
          Logger.error("find actions by session id:" + error)
          error mustEqual 0
      }
    }

    "save a session" in {
      val Some(info) = route(FakeRequest(trackingDomain.routes.TrackingController.saveUserSession()))
      val stringUUID = contentAsJson(info).toString()
      /*println(stringUUID)
      val a = UUID.fromString(stringUUID)*/
      stringUUID.length should be >10

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