package fillDatabase

import models.Genre
import play.api.Logger
import play.api.mvc._

import scala.io.Source

class InsertGenres extends Controller {
  def insertGenres() = Action {
    val lines = Source.fromFile("textFiles/genresIcons").getLines()
//    while (lines.hasNext) {
//      val line = lines.next()
//      try {
//        Genre.save(new Genre(None, line.split("  ")(0), Option(line.split("  ")(1))))
//      } catch {
//        case e: Exception =>  Logger.warn(line + e)
//      }
//    }
    Ok
  }
}