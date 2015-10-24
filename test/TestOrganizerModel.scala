
import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}
import silhouette.UserDAOImpl
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.concurrent.Await

class TestOrganizerModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val geographicPointMethods = new SearchGeographicPoint(dbConfProvider, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val addressMethods = new AddressMethods(dbConfProvider, utilities, geographicPointMethods)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, addressMethods, utilities, geographicPointMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

  "An Organizer" must {

    "be saved and deleted in database" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        try {
          savedOrganizer mustEqual OrganizerWithAddress(
            organizer.copy(
              id = Some(savedOrganizer.organizer.id.get),
              description = Some("<div class='column large-12'>description</div>")),
            None)
        } finally {
          whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "not be saved twice and return the organizerId" in {
      val organizer = Organizer(None, Option("facebookId3"), "organizerTest3", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        try {
          whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds)))
          { _ mustBe savedOrganizer }
        } finally {
          whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "be followed and unfollowed by a user" in {
      val organizer = Organizer(None, Option("facebookId4"), "organizerTest4", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("emaill"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
                timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe true
              }
            }
          } finally {
            whenReady(organizerMethods.unfollow(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "not be followed twice" in {
      val organizer = Organizer(None, Option("facebookId14"), "organizerTest14", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId1", "providerKey1")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName1"),
        lastName = Option("lastName1"),
        fullName = Option("fullName1"),
        email = Option("email1"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
                timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe true
                try {
                  Await.result(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get))
                    , 3 seconds)
                } catch {
                  case e: PSQLException =>

                    e.getSQLState mustBe utilities.UNIQUE_VIOLATION
                }
              }
            }
          } finally {
            whenReady(organizerMethods.unfollow(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "be linked to a place if one with the same facebookId already exists" in {
      whenReady (placeMethods.save(Place(None, "Name1", Some("1234567891"), None, None, None, None, None, None, None)),
        timeout(Span(2, Seconds)))  { tryPlace =>
        val placeId = tryPlace.id.get
        whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(Organizer(None, Some("1234567891"), "organizerTest2"), None)),
          timeout(Span(5, Seconds))) { savedOrganizer =>
          try {
            whenReady(organizerMethods.findById(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) {
              case Some(organizerWithAddress: OrganizerWithAddress) =>
                organizerWithAddress.organizer.linkedPlaceId mustBe Some(placeId)
              case _ =>
                throw new Exception("TestOrganizerModel.musBeLinkedToAPlace: error on save or find")
            }
          } finally {
            organizerMethods.delete(savedOrganizer.organizer.id.get)
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

              organizerMethods.deleteEventRelation(EventOrganizerRelation(savedEvent.id.get, savedOrganizer.id.get))
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

    //find save and delete with address
  }
}

