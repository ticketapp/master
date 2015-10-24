import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.UserDAOImpl
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestPlaceModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val geographicPointMethods = new GeographicPointMethods(dbConfProvider, utilities)
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val userDAOImpl = new UserDAOImpl(dbConfProvider)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, utilities, geographicPointMethods)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  val addressMethods = new AddressMethods(dbConfProvider, utilities, geographicPointMethods)

  "A place" must {

    "be saved and deleted in database and return the new id" in {
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        try {
          whenReady(placeMethods.find(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>
            foundPlace mustBe Option(place.copy(id = foundPlace.get.id,
              description = Some("<div class='column large-12'>Ancienne usine</div>")))
          }
        } finally {
          whenReady(placeMethods.delete(savedPlace.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    /*"be saved with its address and deleted in database" in {
      val address = Address(None, None, Some("privas"), Some("07000"), Some("avignas"))
      val place = Place(None, "test", None, None, None, None, Some(9099), None, None, address = Option(address))

      whenReady(placeMethods.save(place), timeout(Span(5, Seconds))) { savedPlace =>
        try {
          whenReady(placeMethods.find(savedPlace.id.get), timeout(Span(5, Seconds))) { foundPlace =>
            foundPlace mustBe
              Option(place.copy(id = savedPlace.id,
                address = Option(address.copy(geographicPoint = Some("(44.7053439,4.596782999999999)")))))
          }
        } finally {
          placeMethods.delete(savedPlace.id.get)
        }
      }
    }*/

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
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(userDAOImpl.save(user), timeout(Span(2, Seconds))) { userSaved =>
          whenReady(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get))) { resp =>
            whenReady(placeMethods.isFollowed(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp1 =>
             resp1 mustBe true
              whenReady(placeMethods.unfollow(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp2 =>
                resp2 mustBe 1
                userDAOImpl.delete(uuid)
                placeMethods.delete(savedPlace.id.get)
              }
            }
          }
        }
      }
    }

    "not be able to be followed twice" in {
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
      val place = Place(None, "test", Some("123"), None,
        Some("""Ancienne usine"""),
        Some("transbordeur.fr"), Some(9099), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        whenReady(userDAOImpl.save(user), timeout(Span(2, Seconds))) { userSaved =>
          whenReady(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get))) { resp =>
            try {
              Await.result(placeMethods.followByPlaceId(UserPlaceRelation(uuid, savedPlace.id.get)), 3 seconds)
            } catch {
              case e: PSQLException =>

                e.getSQLState mustBe utilities.UNIQUE_VIOLATION
            } finally {
              whenReady(placeMethods.unfollow(UserPlaceRelation(uuid, savedPlace.id.get)), timeout(Span(5, Seconds))) { resp2 =>
                resp2 mustBe 1
                userDAOImpl.delete(uuid)
                placeMethods.delete(savedPlace.id.get)
              }
            }
          }
        }
      }
    }

    "be linked to an organizer if one with the same facebookId already exists" in {
      whenReady(organizerMethods.save(Organizer(None, Some("1234567"), "organizerTestee")), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady (placeMethods.save(Place(None, "Name", Some("1234567"), None, None, None, None, None, None, None)),
          timeout(Span(2, Seconds)))  { tryPlaceId =>
          val placeId = tryPlaceId.id.get
          try {
            whenReady(placeMethods.find(placeId), timeout(Span(5, Seconds))) { foundPlace =>
              foundPlace.get.linkedOrganizerId mustBe Some(savedOrganizer.id.get)
            }
          } finally {
            whenReady(placeMethods.delete(placeId), timeout(Span(5, Seconds))) { resp =>
              resp mustBe 1
              whenReady(organizerMethods.delete(savedOrganizer.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
            }
          }
        }
      }
    }

    "save and delete his relation with an event" in {
      val event = Event(None, None, isPublic = true, isActive = true, "event name",
        Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get),
        Option("description"), new DateTime(), Option(new DateTime(100000000000000L)), 16, None, None, None/*, List.empty, List.empty,
        List.empty, List.empty, List.empty, List.empty*/)
      val place = Place(None, "test1", Some("1231"), None,
        Some("""Ancienne usine1"""),
        Some("transbordeur.fr"), Some(90999), None, Some("https://scontent.xx.fbcdn.net/hphotos.jpg"))
      whenReady(eventMethods.save(event), timeout(Span(5, Seconds))) { savedEvent =>
        whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { tryPlace =>
          val placeId = tryPlace.id.get
          try {
            whenReady(placeMethods.find(placeId), timeout(Span(5, Seconds))) { foundPlace =>
              foundPlace shouldEqual Option(place.copy(id = Option(placeId),
                description = Some("<div class='column large-12'>Ancienne usine1</div>")))
              whenReady(placeMethods.saveEventRelation(EventPlaceRelation(savedEvent.id.get, placeId)),
                timeout(Span(5, Seconds))) { resp =>
                resp mustBe 1
                whenReady(placeMethods.findAllByEvent(savedEvent.id.get), timeout(Span(5, Seconds))) { places =>
                  places should not be empty
                  whenReady(eventMethods.findAllByPlace(placeId), timeout(Span(5, Seconds))) { events1 =>
                    events1 should not be empty
                  }
                }
              }
            }
          } finally {
            whenReady(placeMethods.deleteEventRelation(EventPlaceRelation(savedEvent.id.get, placeId)),
              timeout(Span(5, Seconds))) { resp =>
              resp mustBe 1
              whenReady(placeMethods.delete(placeId), timeout(Span(5, Seconds))) { resp1 =>
                resp1 mustBe 1
                whenReady(eventMethods.delete(savedEvent.id.get), timeout(Span(5, Seconds))) {
                  _ mustBe 1
                }
              }
            }
          }
        }
      }
    }

    "get a new place by facebookId when saving new event by facebookId" in {
      whenReady(eventMethods.saveFacebookEventByFacebookId("933514060052903"), timeout(Span(5, Seconds))) { event =>
        whenReady(placeMethods.getPlaceByFacebookId("836137029786070"), timeout(Span(5, Seconds))) { place =>
          place.name mustBe "Akwaba Coop Culturelle"
          whenReady(placeMethods.delete(place.id.get), timeout(Span(5, Seconds))) { _ mustBe 1}
          whenReady(eventMethods.delete(event.id.get), timeout(Span(5, Seconds))) { _ mustBe 1}
        }
      }
    }
  }
}
