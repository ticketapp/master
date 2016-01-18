package issues

import java.util.UUID
import javax.inject.Inject
import database.{MyPostgresDriver, MyDBTableDefinitions}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import MyPostgresDriver.api._

import scala.concurrent.Future


case class Issue(id: Option[Long] = None, title: String, content: String, userUUID: UUID, fixed: Boolean)

case class IssueComment(content: String, userUUID: UUID, issueId: Long)


class IssueMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with IssueFormsTrait {

  def findAll: Future[Seq[Issue]] = db.run(issues.result)

  def findAllCommentsForIssueId(id: Long): Future[Seq[IssueComment]] =
    db.run(issuesComments.filter(_.issueId === id).result)

  def save(issue: Issue): Future[Issue] =
    db.run(issues returning issues.map(_.id) into ((issue, id) => issue.copy(id = Some(id))) += issue)

  def saveComment(comment: IssueComment): Future[IssueComment] =
    db.run(issuesComments returning issuesComments += comment)
}
