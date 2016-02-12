import java.util.UUID

import database.MyPostgresDriver.api._
import database._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import services.Utilities
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestFollowService extends GlobalApplicationForModelsIntegration with Utilities {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO users(userID, firstName, lastName, fullName, email, avatarURL)
          VALUES ('a4aea509-1002-47d0-b55c-593c91cb32ae', 'simon', 'garnier', 'fullname', 'email0', 'avatarUrl');

        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('200', 'testFindIdByFacebookId', 'facebookUrl00');
        INSERT INTO artists(facebookid, name, facebookurl) VALUES('testFindIdByFacebookId', 'name00', 'testFindIdByFacebookId');

        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime)
          VALUES(100, 'facebookidattendeetest', true, true, 'notPassedEvent3', timestamp '2050-08-24 14:00:00');

        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(100, 'Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');

        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('35894e56-08d1-4c1f-b3e4-466c069d15ed', 'title000', 'url0000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');

        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(300, 'Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');
        """),
      5.seconds)
  }

  val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")

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

            e.getSQLState mustBe UNIQUE_VIOLATION
        }
      }
    }

    "be followed by facebookId" in {
      whenReady(artistMethods.followByFacebookId(userUUID, "testFindIdByFacebookId"), timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")

      whenReady(artistMethods.getFollowedArtists(userUUID), timeout(Span(5, Seconds))) { artists =>
        artists map(_.artist.id) must contain allOf(Some(200), Some(1))
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
      val eventId = 100L
      whenReady(eventMethods.follow(UserEventRelation(userUUID, eventId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        try {
          Await.result(eventMethods.follow(UserEventRelation(userUUID, eventId)), 3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe UNIQUE_VIOLATION
        }
      }
    }

    "be returned if is followed" in {
      whenReady(eventMethods.getFollowedEvents(userUUID), timeout(Span(5, Seconds))) { response =>

        response map(_.event.id) must contain(Some(100))
      }
    }
  }
  
  "An organizer" must {

    "be followed and unfollowed by a user" in {
      val organizer = Organizer(
        name = "organizerTest4",
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)

      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
          timeout(Span(5, Seconds))) { response =>

          response mustBe 1

          whenReady(organizerMethods.isFollowed(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response1 =>

            response1 mustBe true
          }
        }
        whenReady(organizerMethods.unfollow(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
          timeout(Span(5, Seconds))) { response =>

          response mustBe 1
        }
      }
    }

    "be followed by facebookId" in {
      val organizer = Organizer(
        facebookId = Option("facebookId44"),
        name = "organizerTest44",
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)

      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.followByFacebookId(userUUID, savedOrganizer.organizer.facebookId.get),
          timeout(Span(5, Seconds))) { response =>

          response mustBe 1

          whenReady(organizerMethods.isFollowed(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response1 =>

            response1 mustBe true
          }
        }
      }
    }

    "not be followed twice" in {
      val organizer = Organizer(
        name = "organizerTest14",
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)

      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
          timeout(Span(5, Seconds))) { response =>

          response mustBe 1

          whenReady(organizerMethods.isFollowed(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)),
            timeout(Span(5, Seconds))) { response1 =>

            response1 mustBe true
            try {
              Await.result(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, savedOrganizer.organizer.id.get)), 3 seconds)
            } catch {
              case e: PSQLException =>

                e.getSQLState mustBe UNIQUE_VIOLATION
            }
          }
        }
      }
    }
  }
  
  "A place" must {

    "be followed and unfollowed by a user" in {
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
      val placeId = 100L
      whenReady(placeMethods.followByPlaceId(UserPlaceRelation(userUUID, placeId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        try {
          Await.result(placeMethods.followByPlaceId(UserPlaceRelation(userUUID, placeId)), 3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe UNIQUE_VIOLATION
        }
      }
    }

    "be returned if is followed" in {
      whenReady(placeMethods.getFollowedPlaces(userUUID), timeout(Span(5, Seconds))) { response =>
        response map {
          _.place.id
        } must contain(Some(100))
      }
    }
  }

  "A track" must {

    "be followed and unfollowed by a user" in {
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
