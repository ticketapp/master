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
import models.{Track, Artist, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._
import play.api.libs.functional.syntax._
import play.api.Play.current

object InsertFrenchCities extends Controller {
  def insertFrenchCities() = Action {
      val lines = Source.fromFile("textFiles/villes_france.sql").getLines()
      DB.withConnection { implicit connection =>
        var i = 0
        while (lines.hasNext && i < 2000) {
          i = i + 1
          val line = lines.next()
          if (line != "" && line.take(1) == "(") {
            val splitedLine = line.split(",")
            try {
              val cityName: String = splitedLine(4).replaceAll("'", "").trim
              val geographicPoint: String = "(" + splitedLine(19).trim + "," + splitedLine(20).trim + ")"
              SQL(
                s"""INSERT INTO frenchCities(name, geographicPoint)
                  | VALUES ({cityName}, point '$geographicPoint')""".stripMargin)
                .on(
                  'cityName -> cityName,
                  'geographicPoint -> geographicPoint)
                .executeInsert()
            } catch {
              case e: Exception => println(e + splitedLine(4).replaceAll("'", "") + "(" + splitedLine(19).trim + "," + splitedLine(20).trim + ")")
            }
          }
        }
      }
    Ok
  }
}
