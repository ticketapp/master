import models._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.evolutions.Evolutions
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.Utilities

import scala.util.Success

class TestGeographicPointMethods extends PlaySpec with OneAppPerSuite with Injectors with BeforeAndAfterAll {

  override def beforeAll() = {
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() = {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
  }

  "A geographicPoint" must {
    "get a geographicPoint" in {
      val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
      val geoPoint = geographicPointMethods.optionStringToOptionPoint(Option("44.7053439,4.596782999999999"))
      whenReady(geographicPointMethods.getGeographicPoint(address, 3), timeout(Span(2, Seconds))) { addressWithGeoPoint =>
        addressWithGeoPoint.geographicPoint mustBe geoPoint
      }
    }
  }
}