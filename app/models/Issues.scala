package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities.geographicPointToString

case class Issue (issueId: Option[Long], title: String, content: String, userId: String, fixed: Boolean)

case class IssueComment(title: String, content: String, userId: String)

object Issue {
  private val issueParser = {
    get[Long]("issueId") ~
      get[String]("title") ~
      get[String]("content") ~
      get[String]("userId") ~
      get[Boolean]("fixed") map {
      case issueId ~ title ~ content ~ userId ~ fixed =>
        Issue(Option(issueId), title, content, userId, fixed)
    }
  }

  private val issueCommentParser = {
    get[String]("title") ~
      get[String]("content") ~
      get[String]("userId") map {
      case title ~ content ~ userId => IssueComment(title, content, userId)
    }
  }

  def issueFormApply(title: String, content: String, userId: String, fixed: Boolean) =
    new Issue(None, title, content, userId, fixed)
  def issueFormUnapply(issue: Issue) =
    Some((issue.title, issue.content, issue.userId, issue.fixed))

  def findAll: List[Issue] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM issues").as(issueParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.findAll: " + e.getMessage)
  }

  def issueCommentFormApply(title: String, content: String, userId: String) =
    new IssueComment(title, content, userId)
  def issueCommentFormUnapply(issueComment: IssueComment) =
    Some((issueComment.title, issueComment.content, issueComment.userId))

  def findAllCommentForIssueId(issueId: Option[Long]): Seq[IssueComment] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM issuesComments WHERE issueId = {issueId}")
        .on('issueId -> issueId)
        .as(issueCommentParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.findAllCommentForIssueId: " + e.getMessage)
  }
}
