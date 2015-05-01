import java.util.Date
import controllers.DAOException
import models.Organizer
import models.Organizer._
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

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  "An Organizer" must {

    val organizer = Organizer(None, Option("facebookId1"), "organizerTest", Option("description"))

    "be able to be saved and deleted in database and return the new id" in {
      save(organizer) match {
        case None =>
          throw new DAOException("TestOrganizers, error while saving organizer ")
        case Some(organizerId: Long) =>
          find(organizerId) mustEqual Option(organizer.copy(organizerId = Some(organizerId)))
          delete(organizerId) mustBe 1
      }
    }

    "be able to be followed and unfollowed by a user" in {
      followByOrganizerId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByOrganizerId("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      followByOrganizerId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      followByOrganizerId("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollowByOrganizerId("userTestId", 1) mustBe 1
    }
  }
}
