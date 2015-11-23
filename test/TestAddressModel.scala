import addresses.Address
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.GlobalApplicationForModels


class TestAddressModel extends GlobalApplicationForModels {

  "An address" must {

    "not be created if empty" in {

      Address(None, None, Some("jkl"), None, None)
      Address(None, None, None, Some("jkl"), None)
      Address(None, None, None, None, Some("jkl"))

      an[java.lang.IllegalArgumentException] should be thrownBy Option(Address(None, None, None, None, None))
    }

    "be saved (in lowercase) in database and return the new id then be deleted" in {
      val geoPoint = geographicPointMethods.optionStringToOptionPoint(Option("0.0,0.0"))
      val address = Address(None, geoPoint, Option("privas"), Option("07000"), Option("Avignas"))
      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        whenReady(addressMethods.find(savedAddress.id.get), timeout(Span(5, Seconds))) { addressFound =>

          addressFound mustBe Option(address.copy(id = addressFound.get.id, street = Some("Avignas".toLowerCase)))

          whenReady(addressMethods.delete(savedAddress.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "not be saved twice with same city, zip, street and return database addressId on unique violation" in {
      val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
      whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAddress =>
        whenReady(addressMethods.save(address), timeout(Span(5, Seconds))) { savedAgainAddress =>
          savedAgainAddress mustBe Address(Option(savedAddress.id.get),None,Some("privas"),Option("07000"),Some("avignas"))
        }
      }
    }

    "update address" in {
      val address = Address(None, None, Option("coux"), Option("07000"), Option("avignas"))
      val geoPoint = geographicPointMethods.optionStringToOptionPoint(Option("1.0,5.0"))
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
