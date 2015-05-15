package controllers

import models.{IssueComment, Issue}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import json.JsonHelper._

object IssueController extends Controller with securesocial.core.SecureSocial {
  def issues = Action { Ok(Json.toJson(Issue.findAll)) }

  def commentsForIssue(issueId: Long) = Action {
    Ok(Json.toJson(Issue.findAllCommentsForIssueId(Option(issueId))))
  }

  private val issueBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "content" -> nonEmptyText(8)
  )(Issue.issueFormApply)(Issue.issueFormUnapply))

  private val issueCommentBindingForm = Form(mapping(
    "content" -> nonEmptyText(8)
  )(Issue.issueCommentFormApply)(Issue.issueCommentFormUnapply))

  def create = SecuredAction(ajaxCall = true) { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      partialIssue => {
        Ok(Json.toJson(Issue.save(
          Issue(None, partialIssue.title, partialIssue.content, request.user.identityId.userId, fixed = false))))
      }
    )
  }

  def createComment(issueId: Long) = SecuredAction(ajaxCall = true) { implicit request =>
    issueCommentBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      commentContent => {
        Ok(Json.toJson(Issue.saveComment(
          IssueComment(commentContent, request.user.identityId.userId, issueId))))
      }
    )
  }
}
