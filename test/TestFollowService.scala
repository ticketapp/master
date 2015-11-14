import java.util.UUID

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
      whenReady(artistMethods.getFollowedArtists(userUUID), timeout(Span(5, Seconds))) { response =>
        response map { _.id } must contain allOf(Some(200), Some(2))
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
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val organizerId = 100L
      whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, organizerId)), timeout(Span(5, Seconds))) { response =>
        whenReady(organizerMethods.isFollowed(UserOrganizerRelation(userUUID, organizerId)), timeout(Span(5, Seconds))) { response1 =>

          response1 mustBe true
        }
      }
      whenReady(organizerMethods.unfollow(UserOrganizerRelation(userUUID, organizerId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }
    }

    "be followed by facebookId" in {
      val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
      whenReady(organizerMethods.followByFacebookId(userUUID, "facebookId1"), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }
    }

    "not be followed twice" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      val organizerId = 100L
      whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, organizerId)), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        try {
          Await.result(organizerMethods.followByOrganizerId(UserOrganizerRelation(userUUID, organizerId)), 3 seconds)
        } catch {
          case e: PSQLException =>

            e.getSQLState mustBe utilities.UNIQUE_VIOLATION
        }
      }
    }

    "be returned if is followed" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      whenReady(organizerMethods.getFollowedOrganizers(userUUID), timeout(Span(5, Seconds))) { response =>
        response map {
          _.organizer.id
        } must contain(Some(100))
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
