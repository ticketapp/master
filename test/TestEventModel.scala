import java.util.Date

import models.Event.{delete, find, isFollowed, save, _}
import models.Place._
import models._
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._

import services.Utilities.GeographicPoint
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import scala.util.{Failure, Success}

class TestEventModel extends PlaySpec with OneAppPerSuite {

  "An event" must {
    val event = Event(None, None, isPublic = true, isActive = true, "name", Option("(5.4,5.6)"),
      Option("description"), new Date(), Option(new Date(100000000000000L)), 16, None, None, None, List.empty,
      List.empty, List.empty, List.empty, List.empty, List.empty))

    "be saved and deleted in database" in {
      val eventId = save(event.copy(genres = List(Genre(None, "rock", None)))).get

      try {
        find(eventId).get.copy(startTime = new Date(), endTime = None) mustEqual
          event.copy(eventId = Some(eventId), startTime = new Date(), endTime = None)

      } finally {
        delete(eventId) mustBe 1
      }
    }

    "be followed and unfollowed by a user" in {
      val eventId = save(event).get
      try {
        if (follow("userTestId", eventId).isFailure)
          throw new Exception("Event not followed")

        isFollowed(IdentityId("userTestId", "oauth2"), eventId) mustBe true
        unfollow("userTestId", eventId) mustBe Success(1)
      } finally {
        delete(eventId)
      }
    }

    "not be followed twice" in {
      val eventId = save(event).get
      try {

        if (follow("userTestId", eventId).isFailure)
          throw new Exception("Event not followed")

        follow("userTestId", eventId) match {
          case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
          case _ => throw new Exception("follow twice a user worked !")
        }

      } finally {
        unfollow("userTestId", eventId)
        delete(eventId)
      }
    }

    "return events found by genre" in {
      val eventId = save(event).get
      val genre = Genre(None, "rockiedockie", Option("r"))
      Genre.save(genre) match {
        case Some(genreId: Long) =>
          Genre.saveEventRelation(eventId, genreId)
          val eventRock = findAllByGenre("rockiedockie", GeographicPoint("(0,0)"), 0, 1)
          eventRock.get should not be empty

          Genre.deleteEventRelation(eventId, genreId) mustBe Success(1)
          delete(eventId) mustBe 1
          Genre.delete(genreId) mustBe 1
        case _ => throw new Exception("genre could not be saved")
      }
    }

    "return events linked to a place" in {
      val eventId = save(event).get
      whenReady (Place.save(Place(None, "name", Some("12345"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds))) { tryPlaceId =>
        val placeId = tryPlaceId.get.get

        Place.saveEventRelation(eventId, placeId) mustBe true
        findAllByPlace(placeId).head.name mustBe "name"

        Place.deleteEventRelation(eventId, placeId) mustBe Success(1)
        delete(eventId) mustBe 1
        Place.delete(placeId) mustBe Success(1)
      }
    }

    "return passed events for a place" in {
      val eventId = save(event).get

      val passedEvent = Event(None, None, isPublic = true, isActive = true, "passed event", Option("(5.4,5.6)"),
        Option("description"), new Date(0), Option(new Date()), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      val passedEventId = save(passedEvent).get

      whenReady (Place.save(Place(None, "name", Some("12345"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds))) { tryPlaceId =>
        val placeId = tryPlaceId.get.get

        try {
          Place.saveEventRelation(eventId, placeId) mustBe true
          Place.saveEventRelation(passedEventId, placeId) mustBe true

          findAllByPlace(placeId).head.name mustBe "name"
          findAllPassedByPlace(placeId).head.name mustBe "passed event"
        } finally {
          Place.deleteEventRelation(eventId, placeId)
          Place.deleteEventRelation(passedEventId, placeId)
          delete(eventId)
          delete(passedEventId)
        }
      }
    }

    "return passed events for an artist" in {
      val eventId = save(event).get

      val passedEvent = Event(None, None, isPublic = true, isActive = true, "passed event", Option("(5.4,5.6)"),
        Option("description"), new Date(0), Option(new Date()), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)
      val passedEventId = save(passedEvent).get
      val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
        "facebookUrl")
      val artistId = Artist.save(artist).get

      try {
        Artist.saveEventRelation(eventId, artistId) mustBe true
        Artist.saveEventRelation(passedEventId, artistId) mustBe true

        findAllByArtist("facebookUrl").head.name mustBe "name"
        findAllPassedByArtist(artistId).head.name mustBe "passed event"

        Artist.deleteEventRelation(eventId, artistId) mustBe Success(1)
        Artist.deleteEventRelation(passedEventId, artistId) mustBe Success(1)
      } finally {
        Artist.deleteEventRelation(eventId, artistId)
        Artist.deleteEventRelation(passedEventId, artistId)
        delete(eventId)
        delete(passedEventId)
        delete(artistId)
      }
    }

    "return passed events for an organizer" in {
      val eventId = save(event).get
      val passedEventId = save(Event(None, None, isPublic = true, isActive = true, "passed event", Option("(5.4,5.6)"),
        Option("description"), new Date(0), Option(new Date()), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)).get

      val organizerId = Organizer.save(Organizer(None, Option("facebookId10"), "organizerTest2")).get.get

      try {
        Organizer.saveEventRelation(eventId, organizerId) mustBe true
        Organizer.saveEventRelation(passedEventId, organizerId) mustBe true

        findAllByOrganizer(organizerId).head.name mustBe "name"
        findAllPassedByOrganizer(organizerId).head.name mustBe "passed event"

        Organizer.deleteEventRelation(eventId, organizerId) mustBe Success(1)
        Organizer.deleteEventRelation(passedEventId, organizerId) mustBe Success(1)
      } finally {
        delete(eventId) mustBe 1
        delete(passedEventId) mustBe 1
        Organizer.delete(organizerId)
      }
    }

    "return events facebook id for a place facebook id" in {
      whenReady(getEventsFacebookIdByPlace("117030545096697"), timeout(Span(2, Seconds))) {
        _ should not be empty
      }
    }

    "find a complete event by facebookId" in {
      whenReady(findEventOnFacebookByFacebookId("809097205831013"), timeout(Span(5, Seconds))) { event =>
        event.name mustBe "ANNULÉ /// Mad Professor vs Prince Fatty - Dub Attack Tour"
      }
    }

    "have the genre of its artists" in {
      whenReady(Event.findEventOnFacebookByFacebookId("758796230916379"), timeout(Span(5, Seconds))) { event =>
        event.genres should contain allOf (Genre(None, "hip", None), Genre(None, "hop", None))
      }
    }
  }
}
