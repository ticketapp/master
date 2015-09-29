package fillDatabase

import anorm._
import play.api.Logger
import play.api.Play.current

import play.api.mvc._

import scala.io.Source

class InsertFrenchCities extends Controller {
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
              case e: Exception => Logger.error(e + splitedLine(4).replaceAll("'", "") + "(" +
                splitedLine(19).trim + "," + splitedLine(20).trim + ")")
            }
          }
        }
      }
    Ok
  }
}
