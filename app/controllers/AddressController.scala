package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._

import play.api.libs.json.Json
import play.api.mvc._
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import services.Utilities

class AddressController @Inject()(dbConfigProvider: DatabaseConfigProvider,
                      val addressMethods: AddressMethods,
                      val utilities: Utilities) extends Controller {
//  val addressBindingForm = Form(mapping(
//    "city" -> optional(text(2)),
//    "zip" -> optional(text(2)),
//    "street" -> optional(text(2))
//  )(addressMethods.formApply)(addressMethods.formUnapply))
}
