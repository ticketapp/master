import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestFollowService extends GlobalApplicationForModels {

  "An Artist" must {

    "be followed and unfollowed by a user" in {
      whenReady(artistMethods.followByArtistId(UserArtistRelation(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 100L)),
        timeout(Span(5, Seconds))) { resp =>
        whenReady(artistMethods.isFollowed(UserArtistRelation(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 100L)),
          timeout(Span(5, Seconds))) { isFollowed =>

          isFollowed mustBe true

          whenReady(artistMethods.unfollowByArtistId(UserArtistRelation(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 100L)),
            timeout(Span(5, Seconds))) { result =>

            result mustBe 1
          }
        }
      }
    }

    "not be followed twice" in {
      whenReady(artistMethods.followByArtistId(UserArtistRelation(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 200L)),
        timeout(Span(5, Seconds))) { firstResp =>
        try {
          Await.result(artistMethods.followByArtistId(UserArtistRelation(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 200L)),
            3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe utilities.UNIQUE_VIOLATION
        }
      }
    }

    "be follow by facebookId" in {
      val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
      whenReady(artistMethods.followByFacebookId(userUUID, "testFindIdByFacebookId"), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
      whenReady(artistMethods.getFollowedArtists(userUUID), timeout(Span(5, Seconds))) { artists =>
        artists map { _.artist.id } must contain allOf(Some(200), Some(2))
      }
    }
  }

  "An Event" must {

    "be followed and unfollowed by a user" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val eventId = 100L
      whenReady(eventMethods.follow(UserEventRelation(userUUID, eventId)), timeout(Span(5, Seconds))) { response =>
        whenReady(eventMethods.isFollowed(UserEventRelation(userUUID, eventId)), timeout(Span(5, Seconds))) { response1 =>

          response1 mustBe true
        }
      }
      whenReady(eventMethods.unfollow(UserEventRelation(userUUID, eventId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }
    }

    "not be followed twice" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val eventId = 100L
      whenReady(eventMethods.follow(UserEventRelation(userUUID, eventId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        try {
          Await.result(eventMethods.follow(UserEventRelation(userUUID, eventId)), 3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe utilities.UNIQUE_VIOLATION
        }
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      whenReady(eventMethods.getFollowedEvents(userUUID), timeout(Span(5, Seconds))) { response =>
        response map {
          _.event.id
        } must contain(Some(100))
      }
    }
  }
  
  "An organizer" must {


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
          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
            }
          }
          whenReady(organizerMethods.unfollow(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.delete(savedOrganizer.organizer.id.get), timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe 1
            }
          }
        }
      }
    }

    "be followed by facebookId" in {
      val organizer = Organizer(None, Option("facebookId44"), "organizerTest44", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      val loginInfo: LoginInfo = LoginInfo("providerId44", "providerKey44")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName44"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email44"),
        avatarURL = Option("avatarUrl"))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          whenReady(organizerMethods.followByFacebookId(uuid, savedOrganizer.organizer.facebookId.get),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
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
          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.isFollowed(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)),
              timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe true
              try {
                Await.result(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, savedOrganizer.organizer.id.get)), 3 seconds)
              } catch {
                case e: PSQLException =>

                  e.getSQLState mustBe utilities.UNIQUE_VIOLATION
              }
            }
          }
        }
      }
    }
  }
  
  "An place" must {

    "be followed and unfollowed by a user" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val placeId = 100L
      whenReady(placeMethods.followByPlaceId(UserPlaceRelation(userUUID, placeId)), timeout(Span(5, Seconds))) { response =>
        whenReady(placeMethods.isFollowed(UserPlaceRelation(userUUID, placeId)), timeout(Span(5, Seconds))) { response1 =>

          response1 mustBe true
        }
      }
      whenReady(placeMethods.unfollow(UserPlaceRelation(userUUID, placeId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }
    }

    "be followed by facebookId" in {
      val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
      whenReady(placeMethods.followByFacebookId(userUUID, "666137029786070"), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }
    }

    "not be followed twice" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val placeId = 100L
      whenReady(placeMethods.followByPlaceId(UserPlaceRelation(userUUID, placeId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        try {
          Await.result(placeMethods.followByPlaceId(UserPlaceRelation(userUUID, placeId)), 3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe utilities.UNIQUE_VIOLATION
        }
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      whenReady(placeMethods.getFollowedPlaces(userUUID), timeout(Span(5, Seconds))) { response =>
        response map {
          _.place.id
        } must contain(Some(100))
      }
    }
  }

  "A track" must {

    "be followed and unfollowed by a user" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val trackId = UUID.fromString("35894e56-08d1-4c1f-b3e4-466c069d15ed")
      whenReady(trackMethods.followByTrackId(UserTrackRelation(userUUID, trackId)), timeout(Span(5, Seconds))) { response =>
        whenReady(trackMethods.isFollowed(UserTrackRelation(userUUID, trackId)), timeout(Span(5, Seconds))) { response1 =>

          response1 mustBe true
        }
      }
      whenReady(trackMethods.unfollowByTrackId(UserTrackRelation(userUUID, trackId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val trackId = UUID.fromString("35894e56-08d1-4c1f-b3e4-466c069d15ed")
      whenReady(trackMethods.followByTrackId(UserTrackRelation(userUUID, trackId)), timeout(Span(5, Seconds))) { response =>
        whenReady(trackMethods.getFollowedTracks(userUUID), timeout(Span(5, Seconds))) { response =>
          response map {
            _.track.uuid
          } must contain(trackId)
        }
      }
    }
  }
}
