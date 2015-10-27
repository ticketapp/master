package models

import javax.inject.Inject
import controllers.OverQueryLimit
import play.api.Logger
import play.api.libs.ws.{WSResponse, WS}
import services.MyPostgresDriver.api._

import com.vividsolutions.jts.geom.{Geometry, Coordinate, GeometryFactory, Point}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import services.{MyPostgresDriver, Utilities}

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global


class SearchGeographicPoint @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                       val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions with geographicPointTrait {

  def findGeographicPointOfCity(city: String): Future[Option[Geometry]] = {
    val query = frenchCities.filter(_.city === city) map (_.geographicPoint)
    db.run(query.result.headOption)
  }

  def getGeographicPoint(address: Address, retry: Int): Future[Address] = WS
    .url("https://maps.googleapis.com/maps/api/geocode/json")
    .withQueryString(
      "address" -> (address.street.getOrElse("") + " " + address.zip.getOrElse("") + " " + address.city.getOrElse("")),
      "key" -> utilities.googleKey)
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
