package application

import java.io.File
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._

class Admin @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {

  def indexAdmin = Action {
    Ok(views.html.admin.indexAdmin())
  }

  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      val fileName = picture.filename
      val contentType = picture.contentType
      picture.ref.moveTo(new File("/tmp/picture"))
      Ok("File uploaded")
    }.getOrElse {
      Redirect(routes.Application.index()).flashing("error" -> "Missing file")
    }
  }
}
