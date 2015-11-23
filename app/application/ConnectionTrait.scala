package application

import play.api.data.Form
import play.api.data.Forms._

trait ConnectionTrait {

  case class SignInInfo(email: String, password: String, rememberMe: Boolean)

  val signInForm = Form(mapping(
    "email" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  )(SignInInfo.apply)(SignInInfo.unapply))
}
