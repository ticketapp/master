package fillDatabase

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.{IssueMethods, User}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Action

import scala.io.Source


class InitController @Inject() (ws: WSClient,
                                val messagesApi: MessagesApi,
                                val issueMethods: IssueMethods,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry)
    extends Silhouette[User, CookieAuthenticator] {
  
  def init() = {
    insertFrenchCities()
    insertGenres()
    insertTwoLettersArtists()
  }

  def insertFrenchCities() = Action {
    val lines = Source.fromFile("textFiles/villes_france.sql").getLines()
    //      DB.withConnection { implicit connection =>
    //        var i = 0
    //        while (lines.hasNext && i < 2000) {
    //          i = i + 1
    //          val line = lines.next()
    //          if (line != "" && line.take(1) == "(") {
    //            val splitedLine = line.split(",")
    //            try {
    //              val cityName: String = splitedLine(4).replaceAll("'", "").trim
    //              val geographicPoint: String = "(" + splitedLine(19).trim + "," + splitedLine(20).trim + ")"
    //              SQL(
    //                s"""INSERT INTO frenchCities(name, geographicPoint)
    //                  | VALUES ({cityName}, point '$geographicPoint')""".stripMargin)
    //                .on(
    //                  'cityName -> cityName,
    //                  'geographicPoint -> geographicPoint)
    //                .executeInsert()
    //            } catch {
    //              case e: Exception => Logger.error(e + splitedLine(4).replaceAll("'", "") + "(" +
    //                splitedLine(19).trim + "," + splitedLine(20).trim + ")")
    //            }
    //          }
    //        }
    //      }
    Ok
  }

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

  def insertTwoLettersArtists() = Action {
    val chars = "abcdefghijklmnopqrstuvwxyzàçéèëêîïôöùûü0123456789?!$+".toArray
    //    for (c1 <- chars) {
    //      for (c2 <- chars) {
    //        Artist.getEventuallyFacebookArtists(c1.toString + c2.toString) map { artists =>
    //          artists map { Artist.save }
    //        }
    //        Thread.sleep(120)
    //      }
    //    }
    Ok
  }
}

