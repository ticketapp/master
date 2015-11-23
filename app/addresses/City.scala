package addresses

import javax.inject.Inject
import database.{MyPostgresDriver, MyDBTableDefinitions}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.Utilities
import MyPostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps


case class City (id: Option[Int] = None, name: String, geographicPoint: String) {
  require(name.nonEmpty, "It is forbidden to create a city with an empty name.")
}

class CityMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def isACity(pattern: String): Future[Boolean] =
    db.run(frenchCities.filter(_.city === pattern.toLowerCase).exists.result)
}