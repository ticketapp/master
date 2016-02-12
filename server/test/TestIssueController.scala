import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import issues.{Issue, IssueComment}
import json.JsonHelper._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestIssueController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO issues(issueid, title, content, userid, fixed)
          VALUES(100, 'title', 'content', '077f3ea6-2272-4457-a47e-9e9111108e44', false);
        INSERT INTO issuescomments(commentId, content, userid, issueid)
          VALUES(100, 'content', '077f3ea6-2272-4457-a47e-9e9111108e44', 100);
        """),
      5.seconds)
  }

  "Issue controller" should {

    "find all issues" in {
      val expectedIssue = Issue(
        id = Some(100),
        title = "title",
        content = "content",
        userUUID = defaultUserUUID,
        fixed = false)

      val Some(result) = route(FakeRequest(issues.routes.IssueController.issues()))

      status(result) mustEqual OK
      contentAsJson(result).as[Seq[Issue]] mustEqual Seq(expectedIssue)
    }

    "find all comments" in {
      val expectedComment = IssueComment(
        content = "content",
        userUUID = defaultUserUUID,
        issueId = 100)

      val Some(result) = route(FakeRequest(issues.routes.IssueController.commentsForIssue(100)))

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

      val Some(result) = route(FakeRequest(issues.routes.IssueController.create())
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

      val Some(result) = route(FakeRequest(issues.routes.IssueController.commentsForIssue(100)))

      status(result) mustEqual OK
      contentAsJson(result).as[Seq[IssueComment]] mustEqual Seq(expectedComment)
    }
  }
}