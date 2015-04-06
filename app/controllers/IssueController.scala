package controllers

import models.Issue
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._

object IssueController extends Controller {
  def issues = Action { Ok(Json.toJson(Issue.findAll)) }

  def create = Action { }
}
