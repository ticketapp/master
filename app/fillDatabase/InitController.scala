package fillDatabase

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.vividsolutions.jts.geom.Point
import models._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{Failure, Success}


class InitController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi,
                               val issueMethods: IssueMethods,
                               val genreMethods: GenreMethods,
                               val artistMethods: ArtistMethods,
                               val utilities: Utilities,
                               val env: Environment[User, CookieAuthenticator],
                               socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] with HasDatabaseConfigProvider[MyPostgresDriver] {
  
  def init() = {
    insertFrenchCities()
    insertGenres()
    insertTwoLettersArtists()
  }

  case class FrenchCity(name: String, geographicPoint: Point)

  class FrenchCities(tag: Tag) extends Table[FrenchCity](tag, "frenchcities") {
    def name = column[String]("name")
    def geographicPoint = column[Point]("geographicpoint")

    def * = (name, geographicPoint) <> ((FrenchCity.apply _).tupled, FrenchCity.unapply)
  }

  lazy val frenchCities = TableQuery[FrenchCities]

  def insertFrenchCities() = Action {
    val lines = Source.fromFile("textFiles/villes_france.sql").getLines()
    var i = 0
    while (lines.hasNext && i < 2000) {
      i = i + 1
      val line = lines.next()
      if (line != "" && line.take(1) == "(") {
        val splitedLine = line.split(",")
        try {
          val cityName: String = splitedLine(4).replaceAll("'", "").trim
          val geographicPoint: String = splitedLine(19).trim + "," + splitedLine(20).trim

          utilities.stringToGeographicPoint(geographicPoint) match {
            case Success(point) =>
              db.run(frenchCities += FrenchCity(cityName, point))
            case Failure(e) =>
              Logger.error("InitController.insertFrenchCities: ", e)
          }
        } catch {
          case e: Exception => Logger.error(e + splitedLine(4).replaceAll("'", "") + "(" +
            splitedLine(19).trim + "," + splitedLine(20).trim + ")")
        }
      }
    }
    Ok
  }

  def insertGenres() = Action {
    val lines = Source.fromFile("textFiles/genresIcons").getLines()

    while (lines.hasNext) {
      val line = lines.next()
      try {
        genreMethods.save(new Genre(None, line.split("  ")(0), line.split("  ")(1).charAt(0)))
      } catch {
        case e: Exception =>  Logger.warn(line + e)
      }
    }
    Ok
  }

  def insertTwoLettersArtists() = Action {
    val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray
    for (c1 <- chars) {
      for (c2 <- chars) {
        artistMethods.getEventuallyFacebookArtists(c1.toString + c2.toString) map { artists =>
          artists map { artistMethods.save }
        }
        Thread.sleep(120)
      }
    }
    Ok
  }
}

