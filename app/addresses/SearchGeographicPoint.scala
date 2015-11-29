package addresses

import javax.inject.Inject

import application.OverQueryLimit
import com.vividsolutions.jts.geom.Geometry
import database.{MyPostgresDriver, MyDBTableDefinitions}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.ws.WS
import MyPostgresDriver.api._
import services.Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


class SearchGeographicPoint @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with geographicPointTrait
    with Utilities {

  def findGeographicPointOfCity(city: String): Future[Option[Geometry]] = {
    val query = frenchCities.filter(_.city.toLowerCase === city.toLowerCase) map (_.geographicPoint)
    db.run(query.result.headOption)
  }

  def getGeographicPoint(address: Address, retry: Int): Future[Address] = WS
    .url("https://maps.googleapis.com/maps/api/geocode/json")
    .withQueryString(
      "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
      "key" -> googleKey)
    .get()
    .flatMap {
      readGoogleGeographicPoint(_) match {
        case Success(Some(geographicPoint)) =>
          Future { address.copy(geographicPoint = Option(geographicPoint)) }
        case Failure(e: Exception) =>
          Logger.error("Address.getGeographicPoint: ", e)
          Future { address }
        case _ =>
          Future { address }
      }
    } recoverWith {
    case e: OverQueryLimit if retry > 0 =>
      Logger.info("Address.getGeographicPoint: retry: " + retry + " ", e)
      getGeographicPoint(address, retry - 1)
    case e: Exception =>
      Logger.error("Address.getGeographicPoint: ", e)
      Future { address }
  }
}
