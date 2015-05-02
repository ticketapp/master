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
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.util.{Failure, Success}

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  "An Organizer" must {

    val organizer = new Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
      None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
      geographicPoint = Option("(5.4,5.6)"))

    "be able to be saved and deleted in database" in {
      val organizerId = save(organizer).get.get
      find(organizerId) mustEqual Option(organizer.copy(organizerId = Some(organizerId)))

      delete(organizerId) mustBe 1
    }
/*
 Organizer.followByOrganizerId(request.user.identityId.userId, organizerId) match {
      case Success(_) =>
        Created
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
        Status(CONFLICT)("This user already follow this organizer.")
      case Failure(psqlException: PSQLException) if psqlException.getSQLState == FOREIGN_KEY_VIOLATION =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", psqlException)
        Status(CONFLICT)("There is no organizer with this id.")
      case Failure(unknownException) =>
        Logger.error("OrganizerController.followOrganizerByOrganizerId", unknownException)
        Status(INTERNAL_SERVER_ERROR)
    }
 */
    "not be able to be saves twice" in {
      val organizerId = save(organizer).get.get
      val saveStatus = save(organizer)

      saveStatus shouldBe a [Failure[PSQLException]]

      saveStatus match {
        case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        case _ => throw new Exception("save an organizer twice didn't throw a PSQL UNIQUE_VIOLATION")
      }

      delete(organizerId)
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
