package models

import java.util.UUID
import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.DBTableDefinitions

import scala.concurrent.Future

case class Issue(id: Option[Long], title: String, content: String, userUUID: UUID, fixed: Boolean)

case class IssueComment(content: String, userUUID: UUID, issueId: Long)

class IssueMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                     val organizerMethods: OrganizerMethods,
                     val placeMethods: PlaceMethods,
                     val eventMethods: EventMethods,
                     val genreMethods: GenreMethods,
                     val searchSoundCloudTracks: SearchSoundCloudTracks,
                     val searchYoutubeTracks: SearchYoutubeTracks,
                     val trackMethods: TrackMethods,
                     val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with DBTableDefinitions {

  class Issues(tag: Tag) extends Table[Issue](tag, "issues") {
    def id = column[Long]("issueid")
    def title = column[String]("title")
    def content = column[String]("content")
    def userUUID = column[UUID]("userid")
    def fixed = column[Boolean]("fixed")

    def * = (id.?, title, content, userUUID, fixed) <> ((Issue.apply _).tupled, Issue.unapply)

    def aFK = foreignKey("userid", userUUID, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val issues = TableQuery[Issues]

  class IssuesComments(tag: Tag) extends Table[IssueComment](tag, "issuescomments") {
    def content = column[String]("content")
    def userId = column[UUID]("userid")
    def issueId = column[Long]("issueid")

    def * = (content, userId, issueId) <>((IssueComment.apply _).tupled, IssueComment.unapply)

    def aFK = foreignKey("userid", userId, slickUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFK = foreignKey("issueid", issueId, issues)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  lazy val issuesComments = TableQuery[IssuesComments]

  case class PartialIssue(title: String, content: String)

  def issueFormApply(title: String, content: String) =
    new PartialIssue(title, content)

  def issueFormUnapply(partialIssue: PartialIssue) =
    Some((partialIssue.title, partialIssue.content))


  def findAll: Future[Seq[Issue]] = db.run(issues.result)

  def issueCommentFormApply(content: String) = content

  def issueCommentFormUnapply(content: String) = Option(content)

  def findAllCommentsForIssueId(id: Long): Future[Seq[IssueComment]] = db.run(issuesComments.filter(_.issueId === id).result)

  def save(issue: Issue): Future[Issue] = db.run(issues returning issues += issue)

  def saveComment(comment: IssueComment): Future[IssueComment] = db.run(issuesComments returning issuesComments += comment)
}
