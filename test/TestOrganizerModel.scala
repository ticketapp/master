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

import play.api.Play.current

import scala.util.Success
import scala.util.Failure
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits._

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  "An Organizer" must {

    "be saved and deleted in database" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = save(organizer).get.get
      try {
      find(organizerId) mustEqual Option(organizer.copy(organizerId = Some(organizerId),
        description = Some("<div class='column large-12'>description</div>")))

      delete(organizerId) mustBe Success(1)
      } finally {
        delete(organizerId)
      }
    }

    "not be saved twice" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = save(organizer).get.get

      try {
        save(organizer) match {
          case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
          case _ => throw new Exception("save an organizer twice didn't throw a PSQL UNIQUE_VIOLATION")
        }
      } finally {
        delete(organizerId)
      }
    }

    "be followed and unfollowed by a user" in {
      val organizer = Organizer(None, Option("facebookId4"), "organizerTest4", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = save(organizer).get.get
      try {
        followByOrganizerId("userTestId", organizerId)
        isFollowed(IdentityId("userTestId", "oauth2"), organizerId) mustBe true
        unfollowByOrganizerId("userTestId", organizerId) mustBe Success(1)
      } finally {
        delete(organizerId)
      }
    }

    "not be followed twice" in {
      val organizer = Organizer(None, Option("facebookId5"), "organizerTest5", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = save(organizer).get.get

      try {
        followByOrganizerId("userTestId", organizerId)

        followByOrganizerId("userTestId", organizerId) match {
          case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
          case _ => throw new Exception("follow an organizer twice didn't throw a PSQL UNIQUE_VIOLATION")
        }
      } finally {
        unfollowByOrganizerId("userTestId", organizerId)
        delete(organizerId)
      }
    }

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (Place.save(Place(None, "Name", Some("123456789"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlaceId =>
        val placeId = tryPlaceId.get
        val organizerId = save(Organizer(None, Some("123456789"), "organizerTest2")).get.get

        try {
          find(organizerId).get.linkedPlaceId mustBe placeId
        } finally {
          delete(organizerId)
          Place.delete(placeId.get)
        }
      }
    }
  }
}
