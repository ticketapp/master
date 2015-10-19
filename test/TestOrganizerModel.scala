
import java.util.Date

import models._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.Utilities

import scala.util.{Failure, Success}

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val geographicPointMethods = new GeographicPointMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, utilities, geographicPointMethods)

  "An Organizer" must {

    /*"be saved and deleted in database" in {
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

    "not be saved twice and return the organizerId" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = save(organizer)

      try {
        save(organizer) mustBe organizerId
      } finally {
        delete(organizerId.get.get)
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

    "save organizer with event relation" in {
      val organizer = Organizer(Option(666), Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))

      val event = Event(None, None, isPublic = true, isActive = true, "name", Option("(5.4,5.6)"),
        Option("description"), new Date(), Option(new Date(100000000000000L)), 16, None, None, None, List.empty,
        List.empty, List.empty, List.empty, List.empty, List.empty)

      val eventId = Event.save(event).get

      try {
        saveWithEventRelation(organizer, eventId) match {
          case Some(organizerId) =>
            Organizer.deleteEventRelation(eventId, organizerId)
            delete(organizerId)
          case _ =>
            throw new Exception("TestOrganizerModel: saveWithEventRelation didn't work")
        }
      } finally {
          Event.delete(eventId)
      }
    }

    "get the info about the organizer on Facebook" in {
      whenReady (getOrganizerInfo(Option("164354640267171")), timeout(Span(5, Seconds))) { organizerInfos =>
        organizerInfos.get.name mustBe "Le Transbordeur"
      }
    }*/
  }
}

