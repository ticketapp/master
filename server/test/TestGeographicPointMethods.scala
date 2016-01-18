import addresses.Address
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.db.evolutions.Evolutions
import testsHelper.GlobalApplicationForModels


class TestGeographicPointMethods extends GlobalApplicationForModels {

  override def beforeAll() = {
    Evolutions.applyEvolutions(databaseApi.database("tests"))
  }

  override def afterAll() = {
    Evolutions.cleanupEvolutions(databaseApi.database("tests"))
  }

  "A geographicPoint" must {
    "get a geographicPoint" in {
      val address = Address(city = Option("privas"), zip = Option("07000"), street = Option("avignas"))
      val geoPoint = geographicPointMethods.optionStringToPoint(Option("44.735269,4.599038999999999"))
      whenReady(geographicPointMethods.getGeographicPoint(address, 3), timeout(Span(2, Seconds))) { addressWithGeoPoint =>
        addressWithGeoPoint.geographicPoint mustBe geoPoint
      }
    }

    "get a geographicPoint for a city" in {
      whenReady(geographicPointMethods.findGeographicPointOfCity("lyon"), timeout(Span(5, Seconds))) {
        _ mustBe Some(geographicPointMethods.optionStringToPoint(Option("45.783808,4.860598")))
      }
    }
  }
}