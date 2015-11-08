package controllers

import javax.inject.Inject

import models._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

class GenreController @Inject()(val genreMethods: GenreMethods) extends Controller {

  def isAGenre(pattern: String) = Action.async {
    genreMethods.isAGenre(pattern) map { isAGenre =>
      Ok(Json.toJson(isAGenre))
    }
  }
}
