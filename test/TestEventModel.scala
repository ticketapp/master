import java.util.Date

import models.Event.{delete, find, isFollowed, save, _}
import models.Place._
import models.{Event, Place}
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import securesocial.core.IdentityId
import services.Utilities.GeographicPoint
import org.scalatestplus.play._
import org.scalatest._
import Matchers._

import scala.util.{Failure, Success}

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
      follow("userTestId", 1)
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollow("userTestId", 1) mustBe Success(1)
    }

    "not be able to be followed twice" in {
      unfollow("userTestId", 1) mustBe Success(1)
    }

    "be able to be found on facebook by a facebookId" in {
      whenReady (findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(5, Seconds))) { event =>
        event.name mustBe "Mad Professor vs Prince Fatty - Dub Attack Tour"
      }
    }

    "return events found by genre" in {
      val eventRock = findAllByGenre("rock", GeographicPoint("(0,0)"), 0, 1)
      eventRock.get should not be empty
    }

    "return passed events for a place" in {
      val eventId = save(event).get

      val passedEvent = Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
        Option("description"), new Date(0), Option(new Date()), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      val passedEventId = save(passedEvent).get
/*
      whenReady (save(Place(None, "Name", Some("12345"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds))) { tryPlaceId =>
        val placeId = tryPlaceId.get.get
        saveEventRelation(eventId, placeId) mustBe true
      }
*/

//      findAllByPlace()
    }

    "return passed events for an artist" in {

    }

    "return passed events for an organizer" in {

    }
  }
}
