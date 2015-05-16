import java.util.Date
import controllers.DAOException
import models.{Organizer, Address, Place}
import models.Place._
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
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}

class TestPlaceModel extends PlaySpec with OneAppPerSuite {

  "A place" must {

    val place = Place(None, "test", Some("123456789"), None,
      Some("""<div class="column large-12">Ancienne usine destinée à l’origine au traitement des eaux...</div>"""),
      Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"), None)

    "be able to be saved and deleted in database and return the new id" in {
      whenReady(save(place), timeout(Span(2, Seconds))) { placeId =>
        find(placeId.get.get) shouldEqual Option(place.copy(placeId = placeId.get))
        delete(placeId.get.get) mustBe Success(1)
      }
    }

    "be able to be followed and unfollowed by a user" in {
      followByPlaceId("userTestId", 1)
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByPlaceId("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      followByPlaceId("userTestId", 1)
      followByPlaceId("userTestId", 1) match {
        case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
        case _ => throw new Exception("folow a place twice didn't throw a PSQL UNIQUE_VIOLATION")
      }
      unfollowByPlaceId("userTestId", 1) mustBe 1
    }

    "be linked to a place if one with the same facebookId already exists" in {
      val organizerId = Organizer.save(Organizer(None, Some("123456789"), "organizerTest2")).get

      whenReady (save(Place(None, "Name", Some("123456789"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlaceId =>
        val placeId = tryPlaceId.get.get

        find(placeId).get.linkedOrganizerId mustBe organizerId

        delete(placeId) mustBe Success(1)
        Organizer.delete(organizerId.get) mustBe 1
      }
    }
  }
}