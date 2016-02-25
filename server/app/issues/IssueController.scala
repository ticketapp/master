package issues

import javax.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.JsonHelper._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import userDomain.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class IssueController @Inject() (ws: WSClient,
                                    val messagesApi: MessagesApi,
                                    val issueMethods: IssueMethods,
                                    val env: Environment[User, CookieAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] with IssueFormsTrait {

  def issues = Action.async {
    issueMethods.findAll map { comments =>
      Ok(Json.toJson(comments))
    }
  }

  def commentsForIssue(issueId: Long) = Action.async {
    issueMethods.findAllCommentsForIssueId(issueId) map { comments =>
      Ok(Json.toJson(comments))
    }
  }

  def create = SecuredAction.async { implicit request =>
    issueBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("IssueController.create: " + formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      partialIssue => {
        issueMethods.save(Issue(
          id = None,
          title = partialIssue.title,
          content = partialIssue.content,
          userUUID = request.identity.uuid,
          fixed = false)) map { issue =>

          Ok(Json.toJson(issue))
        }
      }
    )
  }

  def createComment(issueId: Long) = SecuredAction.async { implicit request =>
    issueCommentBindingForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.error("IssueController.createComment: " + formWithErrors.errorsAsJson.toString())
        Future(BadRequest(formWithErrors.errorsAsJson))
      },
      commentContent => { issueMethods.saveComment(
        IssueComment(commentContent, request.identity.uuid, issueId)) map { comment =>
          Ok(Json.toJson(comment))
        }
      }
    )
  }
}
