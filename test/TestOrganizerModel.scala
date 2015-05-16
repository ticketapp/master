import java.util.Date
import controllers.DAOException
import models.{Address, Place, Organizer}
import models.Organizer._
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
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  "An Organizer" must {

    val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
      None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
      geographicPoint = Option("(5.4,5.6)"))

    "be able to be saved and deleted in database" in {
      val organizerId = save(organizer).get.get
      find(organizerId) mustEqual Option(organizer.copy(organizerId = Some(organizerId)))

      delete(organizerId) mustBe 1
    }

    "not be able to be saved twice" in {
      val organizerId = save(organizer).get.get
      val saveStatus = save(organizer)

      saveStatus match {
        case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
        case _ => throw new Exception("save an organizer twice didn't throw a PSQL UNIQUE_VIOLATION")
      }

      delete(organizerId)
    }

    "be able to be followed and unfollowed by a user" in {
      followByOrganizerId("userTestId", 1)
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByOrganizerId("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      followByOrganizerId("userTestId", 1)

      followByOrganizerId("userTestId", 1) match {
        case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
        case _ => throw new Exception("follow an organizer twice didn't throw a PSQL UNIQUE_VIOLATION")
      }

      unfollowByOrganizerId("userTestId", 1) mustBe 1
    }

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (Place.save(Place(None, "Name", Some("123456789"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlaceId =>
          val placeId = tryPlaceId.get
          val organizerId = save(Organizer(None, Some("123456789"), "organizerTest2")).get.get

          find(organizerId).get.linkedPlaceId mustBe placeId

          delete(organizerId) mustBe 1
          Place.delete(placeId.get) mustBe Success(1)
      }
    }
  }
}
