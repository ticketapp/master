import java.util.Date
import controllers.DAOException
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

class TestAddressModel extends PlaySpec with OneAppPerSuite {
  "An address" must {

    val address = Address(None, Option("(0.0,0.0)"), Option("city"), Option("zip"), Option("street"))

    "be saved and deleted in database and return the new id" in {
      val addressId = save(Option(address))

      find(addressId) mustEqual Option(address.copy(addressId = addressId))
      delete(addressId.value) mustBe 1
    }

    "not be saved twice and return database addressId on unique violation" in {
      val addressId = save(Option(address))

      save(Option(address)) mustBe addressId
    }

    "not be saved if empty" in {
      save(Option(Address(None, None, None, None, None))) mustBe None
    }
  }
}