package controllers


import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import models._
import play.api.libs.json.Json
import java.io.File

object Admin extends Controller {

  def indexAdmin = Action {
    Ok(views.html.admin.indexAdmin())
  }

  def createOrder(totalPrice: Int): Long = {
    Order.save(totalPrice)
    //Redirect(routes.Admin.indexAdmin())
  }

  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      val filename = picture.filename
      val contentType = picture.contentType
      picture.ref.moveTo(new File("/tmp/picture"))
      Ok("File uploaded")
    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file"
      )
    }
  }
}