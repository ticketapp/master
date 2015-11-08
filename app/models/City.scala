package models

import java.util.UUID
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import services.MyPostgresDriver.api._
import services.{MyPostgresDriver, Utilities}
import scala.concurrent.Future
import scala.language.postfixOps


case class City (id: Option[Int] = None, name: String, geographicPoint: String) {
  require(name.nonEmpty, "It is forbidden to create a city without a name.")
}

class CityMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def isACity(pattern: String): Future[Boolean] = db.run(frenchCities.filter(_.city === pattern).exists.result)

}