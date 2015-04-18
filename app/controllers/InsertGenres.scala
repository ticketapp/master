package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import java.util.regex.Pattern
import anorm._
import play.api.db.DB
import play.api.libs.iteratee.{Concurrent, Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.libs.F.Promise
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Genre, Track, Artist, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._
import play.api.libs.functional.syntax._
import play.api.Play.current

object InsertGenres extends Controller {
  def insertGenres() = Action {
    val lines = Source.fromFile("textFiles/genresIcons").getLines()
    while (lines.hasNext) {
      val line = lines.next()
      try {
        println(line.split("  ")(0))
        println(line.split("  ")(1))
        Genre.save(new Genre(-1L, line.split("  ")(0), Option(line.split("  ")(1))))
      } catch {
        case e: Exception => println(line + e)
      }
    }
    Ok
  }
}