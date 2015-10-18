package models

import javax.inject.Inject
import services.MyPostgresDriver.api._

import com.vividsolutions.jts.geom.Point
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import services.{MyPostgresDriver, Utilities}

import scala.concurrent.Future


class GeographicPointMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def findGeographicPointOfCity(city: String): Future[Option[Point]] = {
    val query = frenchCities.filter(_.city === city) map (_.geographicPoint)
    db.run(query.result.headOption)
  }
}
