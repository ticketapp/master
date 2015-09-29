package models

import java.util.UUID

import anorm.SqlParser._
import anorm._
import controllers._

import play.api.Play.current

case class Issue(issueId: Option[Long], title: String, content: String, userUUID: UUID, fixed: Boolean)

case class IssueComment(content: String, userId: UUID, issueId: Long)

object Issue {
  private val issueParser = {
    get[Long]("issueId") ~
      get[String]("title") ~
      get[String]("content") ~
      get[UUID]("userId") ~
      get[Boolean]("fixed") map {
      case issueId ~ title ~ content ~ userId ~ fixed =>
        Issue(Option(issueId), title, content, userId, fixed)
    }
  }

  private val issueCommentParser = {
      get[String]("content") ~
      get[UUID]("userId") ~
      get[Long]("issueId") map {
      case content ~ userId ~ issueId => IssueComment(content, userId, issueId)
    }
  }

  case class PartialIssue(title: String, content: String)
  def issueFormApply(title: String, content: String) =
    new PartialIssue(title, content)
  def issueFormUnapply(partialIssue: PartialIssue) =
    Some((partialIssue.title, partialIssue.content))


  def findAll: List[Issue] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM issues").as(issueParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.findAll: " + e.getMessage)
  }

  def issueCommentFormApply(content: String) = content
  def issueCommentFormUnapply(content: String) = Option(content)

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
          'userId -> issue.userUUID)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.save: " + e.getMessage)
  }

  def saveComment(comment: IssueComment): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO issuesComments(content, userId, issueId)
          | VALUES ({content}, {userId}, {issueId})""".stripMargin)
        .on(
          'content -> comment.content,
          'userId -> comment.userId,
          'issueId -> comment.issueId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Issue.save: " + e.getMessage)
  }
}
