package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.Play.current
import services.Utilities.geographicPointToString

case class Issue(issueId: Option[Long], title: String, content: String, userId: String, fixed: Boolean)

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
  case class PartialIssue(title: String, content: String)
  def issueFormApply(title: String, content: String) = new PartialIssue(title, content)
  def issueFormUnapply(partialIssue: PartialIssue) = Some((partialIssue.title, partialIssue.content))

  def findAll: List[Issue] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM issues").as(issueParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.findAll: " + e.getMessage)
  }

  case class PartialIssueComment(title: String, content: String)
  def issueCommentFormApply(title: String, content: String) =
    new PartialIssueComment(title, content)
  def issueCommentFormUnapply(partialIssueComment: PartialIssueComment) =
    Some((partialIssueComment.title, partialIssueComment.content))

  def findAllCommentsForIssueId(issueId: Option[Long]): Seq[IssueComment] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM issuesComments WHERE issueId = {issueId}")
        .on('issueId -> issueId)
        .as(issueCommentParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.findAllCommentForIssueId: " + e.getMessage)
  }

  def save(issue: Issue): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO issues(title, content, userId)
          | VALUES ({title}, {content}, {userId})""".stripMargin)
        .on(
          'title -> issue.title,
          'content -> issue.content,
          'userId -> issue.userId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.save: " + e.getMessage)
  }
}
