import java.util.Date
import controllers.{EmptyAddress, DAOException}
import models.Address
import models.Address._
import org.postgresql.util.PSQLException
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

    val address = Address(None, Option("(0.0,0.0)"), Option("city"), Option("zip"), Option("street"))

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

    "not be saved if empty" in {
      save(Option(Address(None, None, None, None, None))) match {
        case Failure(emptyAddress: EmptyAddress) =>
        case _ => throw new Exception("Save an empty address didn't give a failure(EmptyAddress)")
      }
    }
  }
}