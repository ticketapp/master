import addresses.Address
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.GlobalApplicationForModelsIntegration

class AddressModelIntegrationTest extends GlobalApplicationForModelsIntegration {

  "An address" must {
    "be saved (in lowercase) in database and return the new id then be deleted" in {
      val geoPoint = geographicPointMethods.optionStringToPoint(Option("0.0,0.0"))
      val address = Address(None, geoPoint, Option("privas"), Option("07000"), Option("Avignas"))
      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        whenReady(addressMethods.find(savedAddress.id.get), timeout(Span(5, Seconds))) { addressFound =>

          addressFound mustBe Option(address.copy(id = addressFound.get.id, street = Some("Avignas".toLowerCase)))

          whenReady(addressMethods.delete(savedAddress.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "not be saved twice with same city, zip, street and return database addressId on unique violation" in {
      val address = Address(id = None, city = Option("privas"), zip = Option("07000"), street = Option("avignas"))
      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAgainAddress =>

          val expectedAddress = Address(
            id = Option(savedAddress.id.get),
            city = Some("privas"),
            zip = Option("07000"),
            street = Some("avignas"))

          savedAgainAddress mustBe expectedAddress
        }
      }
    }

    "update address" in {
      val address = Address(id = None,
        city = Option("coux"),
        zip = Option("07000"),
        street = Option("avignas"))
      val geoPoint = geographicPointMethods.optionStringToPoint(Option("1.0,5.0"))
      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        val addressWithGeoPoint = address.copy(id = savedAddress.id, geographicPoint = geoPoint)
        whenReady(addressMethods.save(addressWithGeoPoint), timeout(Span(5, Seconds))) { savedAddressWithGeoPoint =>

          savedAddressWithGeoPoint mustBe addressWithGeoPoint

          whenReady(addressMethods.find(savedAddress.id.get), timeout(Span(5, Seconds))) { foundAddress =>

            foundAddress mustEqual Option(savedAddressWithGeoPoint)
          }
        }
      }
    }
  }
}
