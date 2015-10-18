/*
import models.Address
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._

import scala.util.Success

class TestAddressModel extends PlaySpec with OneAppPerSuite {
  "An address" must {

  /*  "not be created if empty" in {
      Address(None, None, Some("jkl"), None, None)
      Address(None, None, None, Some("jkl"), None)
      Address(None, None, None, None, Some("jkl"))
      an [java.lang.IllegalArgumentException] should be thrownBy Option(Address(None, None, None, None, None))
    }

    "be saved (in lowercase) in database and return the new id then be deleted" in {
      val address = Address(None, Option("(0.0,0.0)"), Option("privas"), Option("07000"), Option("Avignas"))
      val addressId = save(Option(address)).get

      find(addressId) mustEqual Option(address.copy(street = Some("Avignas".toLowerCase)))
      delete(addressId.value) mustBe 1
    }

    "not be saved twice with same city, zip, street and return database addressId on unique violation" in {
      val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
      val addressId = save(Option(address)).get

      try {
        save(Option(address)) mustBe Success(addressId)
      } finally {
        delete(addressId.get)
      }
    }

    "should update address" in {
      val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
      val addressId = save(Option(address)).get

      try {
        val addressWithGeoPoint = address.copy(geographicPoint = Some("(1.0,5.0)"))
        save(Option(addressWithGeoPoint))

        find(addressId) mustEqual Option(addressWithGeoPoint)
      } finally {
        delete(addressId.get)
      }
    }

    "get a geographicPoint" in {
      val address = Address(None, None, Option("privas"), Option("07000"), Option("avignas"))
      whenReady(getGeographicPoint(address), timeout(Span(2, Seconds))) {
        case Some(address) =>
        address.geographicPoint mustBe Some("(44.7053439,4.596782999999999)")
        case _ => None
      }
    }*/
  }
}

*/
