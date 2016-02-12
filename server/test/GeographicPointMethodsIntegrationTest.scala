import addresses.Address
import database.MyPostgresDriver.api._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class GeographicPointMethodsIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO frenchcities(city, geographicpoint) VALUES('lyon', '0101000020E6100000ED2B0FD253E446401503249A40711340');"""),
      5.seconds)
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