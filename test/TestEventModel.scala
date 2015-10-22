import java.util.{UUID, Date}
import com.mohiva.play.silhouette.api.LoginInfo
import org.joda.time.DateTime
import models.Place._
import models._
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}
import silhouette.UserDAOImpl
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class TestEventModel extends PlaySpec with OneAppPerSuite {


  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val geographicPointMethods = new GeographicPointMethods(dbConfProvider, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, utilities, geographicPointMethods)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

  "An event" must {

    "be saved and deleted in database" in {
      val event = Event(None, None, isPublic = true, isActive = true, "name",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty,
      List.empty, List.empty, List.empty, List.empty, List.empty)*/)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        try {
          whenReady(eventMethods.find(savedEvent.id.get), timeout(Span(5, Seconds))) { foundEvent =>
            foundEvent.get mustEqual
              event.copy(id = Some(savedEvent.id.get), startTime = foundEvent.get.startTime, endTime =  foundEvent.get.endTime)
          }
        } finally {
          whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "be followed and unfollowed by a user" in {
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email"),
        avatarURL = Option("avatarUrl"))
      val event = Event(None, None, isPublic = true, isActive = true, "name1",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description1"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty,
      List.empty, List.empty, List.empty, List.empty, List.empty)*/)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              whenReady(eventMethods.isFollowed(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response1 =>
                response1 mustBe true
              }
            }
          } finally {
            whenReady(eventMethods.unfollow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>
                response1 mustBe 1
                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) {
                  _ mustBe 1
                }
              }
            }
          }
        }
      }
    }

    "not be followed twice" in {
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
      val event = Event(None, None, isPublic = true, isActive = true, "name2",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description2"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty,
      List.empty, List.empty, List.empty, List.empty, List.empty)*/)
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              try {
                Await.result(eventMethods.follow(UserEventRelation(uuid, savedEvent.id.get)), 3 seconds)
              } catch {
                case e: PSQLException =>

                  e.getSQLState mustBe utilities.UNIQUE_VIOLATION
              }
            }
          } finally {
            whenReady(eventMethods.unfollow(UserEventRelation(uuid, savedEvent.id.get)), timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>
                response1 mustBe 1
                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { response2 =>
                  response2 mustBe 1
                }
              }
            }
          }
        }
      }
    }

    "return events found by genre" in {
      val event = Event(None, None, isPublic = true, isActive = true, "name3",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description3"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty,
      List.empty, List.empty, List.empty, List.empty, List.empty)*/)
      val genre = Genre(None, "rockiedockie", 'r')
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { savedGenre =>
          try {
            whenReady(genreMethods.saveEventRelation(EventGenreRelation(savedEvent.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { genreEventRelation =>
              genreEventRelation mustBe 1
              whenReady(eventMethods.findAllByGenre("rockiedockie", geographicPointMethods.stringToGeographicPoint("5.4,5.6").get, 0, 1),
                timeout(Span(5, Seconds))) { eventsByGenre =>
                eventsByGenre must contain(savedEvent)
              }
            }
          } finally {
            whenReady(genreMethods.deleteEventRelation(EventGenreRelation(savedEvent.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { response =>
              response mustBe 1
              whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) { response1 =>
                response1 mustBe 1
                whenReady(genreMethods.delete(savedGenre.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    /*"return events linked to a place" in {
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
      whenReady(getEventsFacebookIdByPlaceOrOrganizerFacebookId("117030545096697"), timeout(Span(2, Seconds))) {
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
    }*/
  }
}
