
import models._
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val geographicPointMethods = new GeographicPointMethods(dbConfProvider, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, utilities, geographicPointMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)

  "An Organizer" must {

    "be saved and deleted in database" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.save(organizer), timeout(Span(5, Seconds))) { savedOrganizer =>
        try {
          savedOrganizer mustEqual organizer.copy(id = Some(savedOrganizer.id.get),
            description = Some("<div class='column large-12'>description</div>"))
        } finally {
          whenReady(organizerMethods.delete(savedOrganizer.id.get), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "not be saved twice and return the organizerId" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.save(organizer), timeout(Span(5, Seconds))) { savedOrganizer =>
        try {
          whenReady(organizerMethods.save(organizer), timeout(Span(5, Seconds))) { _ mustBe savedOrganizer }
        } finally {
          whenReady(organizerMethods.delete(savedOrganizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    /*"be followed and unfollowed by a user" in {
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
    }*/

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (placeMethods.save(Place(None, "Name1", Some("1234567891"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(organizerMethods.save(Organizer(None, Some("1234567891"), "organizerTest2")),
          timeout(Span(5, Seconds))) { savedOrganizer =>
          try {
            whenReady(organizerMethods.findById(savedOrganizer.id.get), timeout(Span(5, Seconds))) {
              case Some(organizer: Organizer) =>
                organizer.linkedPlaceId mustBe Some(placeId)
              case _ =>
                throw new Exception("TestOrganizerModel.musBeLinkedToAPlace: error on save or find")
            }
          } finally {
            organizerMethods.delete(savedOrganizer.id.get)
            placeMethods.delete(placeId)
          }
        }
      }
    }

    "save organizer with event relation" in {
      val geoPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get)
      val organizer = Organizer(Option(666), Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = geoPoint)

      val event = Event(None, None, isPublic = true, isActive = true, "name", geoPoint,
        Option("description"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty,
        List.empty, List.empty, List.empty, List.empty, List.empty*/)

      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        try {
          whenReady(organizerMethods.saveWithEventRelation(organizer, savedEvent.id.get), timeout(Span(5, Seconds))) { savedOrganizer =>
            whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>
              //
              organizerMethods.deleteEventRelation(savedEvent.id.get, savedOrganizer.id.get)
              organizerMethods.delete(savedOrganizer.id.get)
            }
          }
        } finally {
          eventMethods.delete(savedEvent.id.get)
        }
      }
    }

    "get the info about the organizer on Facebook" in {
      whenReady (organizerMethods.getOrganizerInfo(Option("164354640267171")), timeout(Span(5, Seconds))) { organizerInfos =>
        organizerInfos.get.organizer.name mustBe "Le Transbordeur"
      }
    }
  }
}

