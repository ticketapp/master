import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import json.JsonHelper._
import models._
import play.api.libs.json._
import play.api.test.FakeRequest

import scala.language.postfixOps


class TestIssueController extends GlobalApplicationForControllers {
  sequential

  "Issue controller" should {

    "find all issues" in {
      val expectedIssue = Issue(
        id = Some(100),
        title = "title",
        content = "content",
        userUUID = defaultUserUUID,
        fixed = false)

      val Some(result) = route(FakeRequest(controllers.routes.IssueController.issues()))

      status(result) mustEqual OK
      contentAsJson(result).as[Seq[Issue]] mustEqual Seq(expectedIssue)
    }

    "find all comments" in {
      val expectedComment = IssueComment(
        content = "content",
        userUUID = defaultUserUUID,
        issueId = 100)

      val Some(result) = route(FakeRequest(controllers.routes.IssueController.commentsForIssue(100)))

      status(result) mustEqual OK
      contentAsJson(result).as[Seq[IssueComment]] mustEqual Seq(expectedComment)
    }

    "create an issue" in {
      val jsonIssue = Json.parse(
        """{
          |  "title": "title",
          |  "content": "contentOfMoreThan8Characters"
        }""".stripMargin)

      val expectedIssue = Issue(
        id = Some(1),
        title = "title",
        content = "contentOfMoreThan8Characters",
        userUUID = defaultUserUUID,
        fixed = false)

      val Some(result) = route(FakeRequest(controllers.routes.IssueController.create())
        .withJsonBody(jsonIssue)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      contentAsJson(result).as[Issue] mustEqual expectedIssue
    }

    "create a comment" in {
      val expectedComment = IssueComment(
        content = "content",
        userUUID = defaultUserUUID,
        issueId = 100)

      val Some(result) = route(FakeRequest(controllers.routes.IssueController.commentsForIssue(100)))

      status(result) mustEqual OK
      contentAsJson(result).as[Seq[IssueComment]] mustEqual Seq(expectedComment)
    }
  }
}