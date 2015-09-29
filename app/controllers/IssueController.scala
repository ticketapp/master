package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import models.{IssueMethods, User, Issue, IssueComment}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IssueController @Inject() (ws: WSClient,
                                    val messagesApi: MessagesApi,
                                    val issueMethods: IssueMethods,
                                    val env: Environment[User, CookieAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def issues =  Action.async {
    issueMethods.findAll map { comments =>
      Ok(Json.toJson(comments))
    }
  }

  def commentsForIssue(issueId: Long) = Action.async {
    issueMethods.findAllCommentsForIssueId(issueId) map { comments =>
      Ok(Json.toJson(comments))
    }
  }

  private val issueBindingForm = Form(mapping(
    "title" -> nonEmptyText(2),
    "content" -> nonEmptyText(8)
  )(issueMethods.issueFormApply)(issueMethods.issueFormUnapply))

  private val issueCommentBindingForm = Form(mapping(
    "content" -> nonEmptyText(8)
  )(issueMethods.issueCommentFormApply)(issueMethods.issueCommentFormUnapply))

  def create = SecuredAction.async { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      partialIssue => {
        issueMethods.save(
          Issue(None, partialIssue.title, partialIssue.content, request.identity.userID.toString, fixed = false)) map { issues =>
          Ok(Json.toJson(issues))
        }
      }
    )
  }

  def createComment(issueId: Long) = SecuredAction.async { implicit request =>
    issueCommentBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error(formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      commentContent => { issueMethods.saveComment(
        IssueComment(commentContent, request.identity.userID.toString, issueId)) map { comment =>
          Ok(Json.toJson(comment))
        }
      }
    )
  }
}
