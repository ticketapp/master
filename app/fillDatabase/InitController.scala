package fillDatabase

import javax.inject.Inject

import addresses.SearchGeographicPoint
import application.User
import artistsDomain.ArtistMethods
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import database.MyPostgresDriver.api._
import database.{FrenchCity, MyDBTableDefinitions, MyPostgresDriver}
import genresDomain.{Genre, GenreMethods}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


class InitController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val messagesApi: MessagesApi,
                               val genreMethods: GenreMethods,
                               val artistMethods: ArtistMethods,
                               val geographicPointMethods: SearchGeographicPoint,
                               val env: Environment[User, CookieAuthenticator],
                               socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator]
    with HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions {
  
  def init() = {
    insertFrenchCities()
    insertGenres()
//    insertTwoLettersArtists()
  }

  def insertFrenchCities() = Action {
    val lines = Source.fromFile("textFiles/villes_france.sql").getLines()
    var i = 0
    while (lines.hasNext) {
      Thread.sleep(200)
      i = i + 1
      val line = lines.next()
      if (line != "" && line.take(1) == "(") {
        val splitedLine = line.split(",")
        Try {
          val cityName: String = splitedLine(4).replaceAll("'", "").trim
          val geographicPoint: String = splitedLine(19).trim + "," + splitedLine(20).trim

          geographicPointMethods.stringToGeographicPoint(geographicPoint) match {
            case Success(point) =>
              db.run(frenchCities += FrenchCity(cityName, point))
            case Failure(e) =>
              Logger.error("InitController.insertFrenchCities: ", e)
          }
        } match {
          case Failure(e: Exception) =>
            Logger.error(e + splitedLine(4).replaceAll("'", "") + "(" + splitedLine(19).trim + "," + splitedLine(20).trim + ")")
          case _ =>
        }
      }
    }
    Ok
  }

  def insertGenres() = Action {
    val lines = Source.fromFile("textFiles/genresIcons").getLines()

    while (lines.hasNext) {
      Thread.sleep(200)
      val line = lines.next()
      genreMethods.save(new Genre(None, line.split("  ")(0), line.split("  ")(1).charAt(0))) recover {
        case NonFatal(e: Exception) =>  Logger.warn("InitController.insertGenres: " + line + e)
      }
    }
    Ok
  }

  def insertTwoLettersArtists() = Action {
    val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray
    for (c1 <- chars) {
      for (c2 <- chars) {
        Thread.sleep(2000)
        artistMethods.getEventuallyFacebookArtists(c1.toString + c2.toString) map { artists =>
          artists map { artist => artistMethods.save(artist) }
        } recover {
          case NonFatal(e: Exception) =>  Logger.warn("InitController.insertTwoLettersArtists: for pattern " + c1 + c2)
        }
      }
    }
    Ok
  }
}

