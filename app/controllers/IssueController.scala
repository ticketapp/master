package controllers

import models.Issue
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import json.JsonHelper._

object IssueController extends Controller with securesocial.core.SecureSocial {
  def issues = Action { Ok(Json.toJson(Issue.findAll)) }

  private val issueBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "content" -> nonEmptyText(8)
  )(Issue.issueFormApply)(Issue.issueFormUnapply))

  private val issueCommentBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "content" -> nonEmptyText(8)
  )(Issue.issueCommentFormApply)(Issue.issueCommentFormUnapply))

  def create = SecuredAction(ajaxCall = true) { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      partialIssue => {
        Ok(Json.toJson(Issue.save(
          Issue(None, partialIssue.title, partialIssue.content, request.user.identityId.userId, fixed = false))))
      }
    )
  }

  /*def createComment = SecuredAction(ajaxCall = true) { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      partialCommentIssue => {
        Ok(Json.toJson(Issue.save(
          Issue(None, partialIssue.title, partialIssue.content, request.user.identityId.userId, fixed = false))))
      }
    )
  }*/
}
