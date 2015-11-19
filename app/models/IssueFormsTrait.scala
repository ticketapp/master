package models

import play.api.data.Form
import play.api.data.Forms._

trait IssueFormsTrait {
  case class PartialIssue(title: String, content: String)

  def issueFormApply(title: String, content: String) = new PartialIssue(title, content)

  def issueFormUnapply(partialIssue: PartialIssue) = Some((partialIssue.title, partialIssue.content))

  def issueCommentFormApply(content: String) = content

  def issueCommentFormUnapply(content: String) = Option(content)

  val issueBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "content" -> nonEmptyText(8)
  )(issueFormApply)(issueFormUnapply))

  val issueCommentBindingForm = Form(mapping(
    "content" -> nonEmptyText(8)
  )(issueCommentFormApply)(issueCommentFormUnapply))
}
