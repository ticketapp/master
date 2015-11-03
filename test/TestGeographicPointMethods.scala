import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.db.evolutions.Evolutions

class TestGeographicPointMethods extends GlobalApplicationForModels {

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

    "get a geographicPoint for a city" in {
      whenReady(geographicPointMethods.findGeographicPointOfCity("lyon"), timeout(Span(5, Seconds))) {
        _ mustBe geographicPointMethods.optionStringToOptionPoint(Option("45.783808,4.860598"))
      }
    }
  }
}