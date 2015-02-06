package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.db._
import play.api.Play.current
import anorm._
import play.api.mvc._
import play.api.libs.json.Json
import models.User
//import java.util.Date

object UserController extends Controller {
  def users = Action {
    Ok(Json.toJson(User.findAll()))
  }

  def user(id: Long) = Action {
    Ok(Json.toJson(User.find(id)))
  }

  def findUsersContaining(pattern: String) = Action {
    Ok(Json.toJson(User.findAllContaining(pattern)))
  }

  val userBindingForm = Form(mapping(
    "email" -> email,
    "nickname" -> nonEmptyText(2),
    "password" -> nonEmptyText(2),
    "profile" -> nonEmptyText(2)
  )(User.formApply)(User.formUnapply)
  )

  def createUser = Action { implicit request =>
    userBindingForm.bindFromRequest().fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      user => {
        User.saveUser(user)
        Redirect(routes.UserController.user(1))
      }
    )
  }
  
  def deleteUser(userId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM users WHERE userId={userId}").on(
        'userId -> userId
      ).executeUpdate()
    }
  }
}
