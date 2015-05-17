import java.text.{DateFormat, SimpleDateFormat}
import controllers.DAOException
import models.Event._
import models.{Organizer, Address, Event}
import org.postgresql.util.PSQLException
import org.scalatest.time.{Span, Seconds}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import services.Utilities.GeographicPoint
import scala.concurrent.Future
import scala.util.{Success, Failure}
import play.api.libs.concurrent.Execution.Implicits._
import org.scalatest.concurrent.ScalaFutures._
import java.util.Date

class TestEventModel extends PlaySpec with OneAppPerSuite {

  "An event" must {
    val event = Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
      Option("description"), new Date(), Option(new Date()), 16, None, None, None, List.empty, List.empty,
      List.empty, List.empty, List.empty, List.empty)

    "be able to be saved and deleted in database" in {
      val eventId = save(event).get

      find(eventId).get.name mustBe "event name"

      delete(eventId) mustBe 1
      //find(eventId) mustEqual Option(event.copy(eventId = Some(eventId)))
      //pb with dates
    }

    "be able to be followed and unfollowed by a user" in {
      follow("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollow("userTestId", 1) mustBe Success(1)
    }

    "not be able to be followed twice" in {
      follow("userTestId", 1) shouldBe a [Success[Option[Long]]]
      follow("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollow("userTestId", 1) mustBe Success(1)
    }

    "be able to be found on facebook by a facebookId" in {
      whenReady (findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(5, Seconds))) { event =>
        event.name mustBe "Mad Professor vs Prince Fatty - Dub Attack Tour @ Club Transbo"
      }
    }

    "return events found by genre" in {
      val eventRock = findAllByGenre("rock", GeographicPoint("(0,0)"), 0, 1)
      eventRock.get should not be empty
    }
  }
}
