import database.MyPostgresDriver.api._
import issues.{Issue, IssueComment}
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestIssueModel extends GlobalApplicationForModelsIntegration {

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

  "An issue" must {

    "all be found" in {
      val expectedIssue = Issue(
        id = Some(100),
        title = "title",
        content = "content",
        userUUID = defaultUserUUID,
        fixed = false)

      whenReady(issueMethods.findAll, timeout(Span(5, Seconds))) { issues =>
        issues mustBe Seq(expectedIssue)
      }
    }

    "found all its comments" in {
      val expectedComment = IssueComment(
        content = "content",
        userUUID = defaultUserUUID,
        issueId = 100)

      whenReady(issueMethods.findAllCommentsForIssueId(100), timeout(Span(5, Seconds))) { comments =>
        comments mustBe Seq(expectedComment)
      }
    }

    "save an issue" in {
      val issue = Issue(
        id = None,
        title = "title",
        content = "content",
        userUUID = defaultUserUUID,
        fixed = false)

      whenReady(issueMethods.save(issue), timeout(Span(5, Seconds))) { savedIssue =>
        savedIssue mustBe issue.copy(id = Some(1))
        whenReady(issueMethods.findAll, timeout(Span(5, Seconds))) { issues =>
          issues should contain(issue.copy(id = Some(1)))
        }
      }
    }

    "save a comment" in {
      val comment = IssueComment(
        content = "content",
        userUUID = defaultUserUUID,
        issueId = 100)

      whenReady(issueMethods.saveComment(comment), timeout(Span(5, Seconds))) { savedComment =>
        savedComment mustBe comment
        whenReady(issueMethods.findAllCommentsForIssueId(100), timeout(Span(5, Seconds))) { comments =>
          comments should contain(comment)
        }
      }
    }
  }
}
