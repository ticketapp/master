import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

import scala.util.Success

class TestAddressModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val addressMethods = new AddressMethods(dbConfProvider, new Utilities)

  "An address" must {

    "not be created if empty" in {
      Address(None, None, Some("jkl"), None, None)
      Address(None, None, None, Some("jkl"), None)
      Address(None, None, None, None, Some("jkl"))
      an [java.lang.IllegalArgumentException] should be thrownBy Option(Address(None, None, None, None, None))
    }

    "be saved (in lowercase) in database and return the new id then be deleted" in {
      val address = Address(None, Option("(0.0,0.0)"), Option("privas"), Option("07000"), Option("Avignas"))

      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        addressMethods.find(savedAddress.id.get) mustEqual Option(address.copy(street = Some("Avignas".toLowerCase)))
        addressMethods.delete(savedAddress.id.get) mustBe 1
      }
    }

    "not be saved twice with same city, zip, street and return database addressId on unique violation" in {
     val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
     whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
       try {
         whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { _ mustBe Success(savedAddress.id.get) }
       } finally {
         addressMethods.delete(savedAddress.id.get)
       }
     }
    }

    "should update address" in {
     val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
     whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
       try {
         val addressWithGeoPoint = address.copy(geographicPoint = Some("(1.0,5.0)"))
         addressMethods.save(addressWithGeoPoint)
         addressMethods.find(savedAddress.id.get) mustEqual Option(addressWithGeoPoint)
       } finally {
         addressMethods.delete(savedAddress.id.get)
       }
     }
    }

    "get a geographicPoint" in {
     val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
     whenReady(addressMethods.getGeographicPoint(address, 3), timeout(Span(2, Seconds))) { addressWithGeoPoint =>
       addressWithGeoPoint.geographicPoint mustBe Some("(44.7053439,4.596782999999999)")
     }
    }
  }
}


