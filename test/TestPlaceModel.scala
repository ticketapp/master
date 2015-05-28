import java.util.Date

import models.Place._
import models.{Address, Event, Organizer, Place}
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import securesocial.core.IdentityId
import services.Utilities.UNIQUE_VIOLATION
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Failure, Success}

class TestPlaceModel extends PlaySpec with OneAppPerSuite {

  "A place" must {

    val place = Place(None, "test", Some("123"), None,
      Some("""<div class="column large-12">Ancienne usine destinée à l’origine au traitement des eaux...</div>"""),
      Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
/*
    "be saved and deleted in database and return the new id" in {
      whenReady(save(place), timeout(Span(2, Seconds))) { placeId =>
        find(placeId.get.get) mustBe Option(place.copy(placeId = placeId.get))
        delete(placeId.get.get) mustBe Success(1)
      }
    }

    "be saved with its address and deleted in database" in {
      val address = Address(None, None, Some("privas"), Some("07000"), Some("avignas"))
      val place = Place(None, "test", None, None, None, None, Some(9099), None, None, address = Option(address))

      whenReady(save(place), timeout(Span(2, Seconds))) { placeId =>
        try {
          find(placeId.get.get) mustBe
            Option(place.copy(placeId = placeId.get, address = Option(address.copy(geographicPoint = Some("(4,5")))))
        } catch {
          case e: Exception => throw e
        } finally {
          delete(placeId.get.get)
        }
      }
    }

    "be followed and unfollowed by a user" in {
      followByPlaceId("userTestId", 1)
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByPlaceId("userTestId", 1) mustBe Success(1)
    }

    "not be able to be followed twice" in {
      followByPlaceId("userTestId", 1)
      followByPlaceId("userTestId", 1) match {
        case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
        case _ => throw new Exception("follow a place twice didn't throw a PSQL UNIQUE_VIOLATION")
      }
      unfollowByPlaceId("userTestId", 1) mustBe Success(1)
    }

    "be linked to an organizer if one with the same facebookId already exists" in {
      val organizerId = Organizer.save(Organizer(None, Some("1234567"), "organizerTestee")).get

      whenReady (save(Place(None, "Name", Some("1234567"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlaceId =>
        val placeId = tryPlaceId.get.get

        find(placeId).get.linkedOrganizerId mustBe organizerId

        delete(placeId) mustBe Success(1)
        Organizer.delete(organizerId.get) mustBe 1
      }
    }

    "save and delete his relation with an event" in {
      val eventId = Event.save(Event(None, None, isPublic = true, isActive = true, "event name", Option("(5.4,5.6)"),
        Option("description"), new Date(), Option(new Date(100000000000000L)), 16, None, None, None, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty)).get

      whenReady(save(place), timeout(Span(2, Seconds))) { tryPlaceId =>
        val placeId = tryPlaceId.get.get

        find(placeId) shouldEqual Option(place.copy(placeId = Option(placeId)))

        saveEventRelation(eventId, placeId) mustBe true

        findAllByEvent(eventId) should not be empty
        Event.findAllByPlace(placeId) should not be empty
        deleteEventRelation(eventId, placeId) mustBe Success(1)

        delete(placeId) mustBe Success(1)
        Event.delete(eventId) mustBe 1
      }
    }
*/
    "get the geoPoint" in {
      val address1 = Address(None, Some("(1.0,1.0)"), None, None, None)
      val address2 = None
      val address3 = Address(None, None, Some("privas"), Some("07000"), Some("avignas"))

      whenReady(saveAddressInFutureWithGeoPoint(Option(address1)), timeout(Span(5, Seconds))) {
        case Success(Some(addressId)) => Address.find(Option(addressId)) mustBe Some(address1)
        case _ => throw new Exception("address not saved")
      }

      whenReady(saveAddressInFutureWithGeoPoint(address2), timeout(Span(5, Seconds))) {
        case Success(None) =>
        case _ => throw new Exception("address not saved")
      }

      whenReady(saveAddressInFutureWithGeoPoint(Option(address3)), timeout(Span(5, Seconds))) {
        case Success(Some(addressId)) => Address.find(Option(addressId)) mustBe
          Some(address3.copy(geographicPoint = Some("(44.7053439,4.596782999999999)")))
        case _ => throw new Exception("address not saved")
      }
    }
  }
}
