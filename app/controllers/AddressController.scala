package controllers

import anorm._
import models._
import json.JsonHelper._
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current

object AddressController extends Controller {
  val addressBindingForm = Form(mapping(
    "city" -> optional(text(2)),
    "zip" -> optional(text(2)),
    "street" -> optional(text(2))
  )(Address.formApply)(Address.formUnapply))
}
