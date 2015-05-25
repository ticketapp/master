import java.util.Date
import controllers.{EmptyAddress, DAOException}
import models.Address
import models.Address._
import models.Address.delete
import models.Address.find
import models.Address.save
import models.Place
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import org.scalatest.OptionValues._
import services.Utilities.UNIQUE_VIOLATION
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Failure, Success}

class TestAddressModel extends PlaySpec with OneAppPerSuite {
  "An address" must {

    val address = Address(None, Option("(0.0,0.0)"), Option("privas"), Option("07000"), Option("Avignas"))

    "be saved and deleted in database and return the new id" in {
      val addressId = save(Option(address)).get

      find(addressId) mustEqual Option(address.copy(addressId = addressId))
      delete(addressId.value) mustBe 1
    }

    "not be saved twice with same city, zip, street and return database addressId on unique violation" in {
      val addressId = save(Option(address)).get

      save(Option(address))
      save(Option(address)) match {
        case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
        case _ => throw new Exception("save an address twice didn't throw a PSQL UNIQUE_VIOLATION")
      }

      delete(addressId.get)
    }

    "not be created if empty" in {
      an [java.lang.IllegalArgumentException] should be thrownBy Option(Address(None, None, None, None, None))
    }

    "get a geographicPoint" in {
      whenReady(getGeographicPoint(address.copy(geographicPoint = None)), timeout(Span(2, Seconds))) { address =>
        address.geographicPoint mustBe Some("(44.7053439,4.596782999999999)")
      }
    }
  }
}