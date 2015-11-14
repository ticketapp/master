import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.Matchers._


class TestUserModel extends GlobalApplicationForModels {

  "A user" must {

    "be saved and deleted" in {
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

      whenReady(userDAOImpl.save(user), timeout(Span(2, Seconds))) { userSaved =>

        userSaved mustBe user

        whenReady(userDAOImpl.find(user.loginInfo), timeout(Span(2, Seconds))) { userFound =>
          userFound mustBe Option(userSaved)
          whenReady(userDAOImpl.delete(user.uuid), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "get his followed places" in {
      val place = Place(None, "test", None, None, None, None, None, None, None)
      val uuid: UUID = UUID.randomUUID()
      val loginInfo: LoginInfo = LoginInfo("providerId1", "providerKey1")
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName1"),
        lastName = Option("lastName1"),
        fullName = Option("fullName1"),
        email = Option("email1"),
        avatarURL = Option("avatarUrl1"))

      whenReady(placeMethods.save(place), timeout(Span(2, Seconds))) { savedPlace =>
        val placeId = savedPlace.id.get
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          whenReady(placeMethods.followByPlaceId(UserPlaceRelation(uuid, placeId)),
            timeout(Span(5, Seconds))) { resp =>
            whenReady(placeMethods.getFollowedPlaces(uuid), timeout(Span(5, Seconds))) { followedPlaces =>

              followedPlaces should contain (savedPlace)
              whenReady(placeMethods.unfollow(UserPlaceRelation(uuid, placeId))) { isDeletePlace =>

                isDeletePlace mustBe 1

                whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { _ mustBe 1}
              }
            }
          }
        }
      }
    }

    "get his followed organizers" in {
      val uuid: UUID = UUID.randomUUID()
      val loginInfo: LoginInfo = LoginInfo("providerId11", "providerKey11")
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName11"),
        lastName = Option("lastName11"),
        fullName = Option("fullName11"),
        email = Option("email11"),
        avatarURL = Option("avatarUrl11"))
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option(geographicPointMethods.stringToGeographicPoint("5.4,5.6").get))
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        val organizerId = savedOrganizer.organizer.id.get
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>

          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, organizerId)),
            timeout(Span(5, Seconds))) { response =>

              response mustBe 1

            whenReady(organizerMethods.getFollowedOrganizers(uuid), timeout(Span(5, Seconds))) { organizers =>

                organizers must contain (savedOrganizer)
            }
          }
          whenReady(organizerMethods.unfollow(UserOrganizerRelation(uuid, organizerId)),
            timeout(Span(5, Seconds))) { response =>

            response mustBe 1

            whenReady(organizerMethods.delete(organizerId), timeout(Span(5, Seconds))) { response1 =>

              response1 mustBe 1

              whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { _ mustBe 1}
            }
          }
        }
      }
    }
//
//    "get tracks he had removed" in {
//      val artist = Artist(None, Option("facebookIdTestUserModel"), "artistTest", Option("imagePath"),
//        Option("description"), "artistFacebookUrlTestUserModel", Set("website"))
//      val artistId = Artist.save(artist).get
//      val trackId = randomUUID
//      val track = Track(trackId, "titleTestUserModel", "url2", 's', "thumbnailUrl",
//        "artistFacebookUrlTestUserModel", "artistName")
//
//      try {
//        Track.save(track)
//        Track.upsertRatingDown("userTestId", trackId, -2, Some('r'))
//
//        getTracksRemoved("userTestId") mustBe Seq(track.copy(confidence = Some(-0.002)))
//
//      } finally {
//        Track.deleteRatingForUser("userTestId", trackId)
//        Track.delete(trackId)
//        Artist.delete(artistId)
//      }
//    }
  }
}
