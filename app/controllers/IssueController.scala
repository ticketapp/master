package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models.{Issue, IssueComment, User}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

class IssueController @Inject() (ws: WSClient,
                                    val messagesApi: MessagesApi,
                                    val env: Environment[User, CookieAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

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

  def create = SecuredAction { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      partialIssue => {
        Ok(Json.toJson(Issue.save(
          Issue(None, partialIssue.title, partialIssue.content, request.identity.UUID, fixed = false))))
      }
    )
  }

  def createComment(issueId: Long) = SecuredAction { implicit request =>
    issueCommentBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        BadRequest(formWithErrors.errorsAsJson)
      },
      commentContent => {
        Ok(Json.toJson(Issue.saveComment(
          IssueComment(commentContent, request.identity.UUID, issueId))))
      }
    )
  }
}
