package controllers

import models.User
import org.joda.time.DateTime
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
//import models.User
//java.util.Date

object UserController extends Controller {
  def users = Action {
    Ok(Json.toJson(User.findAll()))
  }

  def user(id: Long) = Action {
    Ok(Json.toJson(User.find(id)))
  }

  def deleteUser(userId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users WHERE userId={userId}").on(
        'userId -> userId
      ).executeUpdate()
    }
  }
}
