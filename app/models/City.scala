package models

import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.MyPostgresDriver
import services.MyPostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps


case class City (id: Option[Int] = None, name: String, geographicPoint: String) {
  require(name.nonEmpty, "It is forbidden to create a city without a name.")
}

class CityMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def isACity(pattern: String): Future[Boolean] = db.run(frenchCities.filter(_.city === pattern).exists.result)
}