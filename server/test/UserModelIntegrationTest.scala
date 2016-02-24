import java.util.UUID

import application.{GuestUser, User}
import com.mohiva.play.silhouette.api.LoginInfo
import database.MyPostgresDriver.api._
import database.UserOrganizerRelation
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import organizersDomain.{Organizer, OrganizerWithAddress}
import testsHelper.GlobalApplicationForModelsIntegration

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class UserModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO guestUsers(ip) VALUES ('127.0.0.0');
        INSERT INTO guestUsers(ip) VALUES ('127.0.0.6');
        INSERT INTO guestUsers(ip) VALUES ('127.0.0.7');
        INSERT INTO places(placeid, name, facebookid)
          VALUES(400, 'testId4BecauseThereIsTRANSBORDEUR', 'facebookIdTestFollowController');
        INSERT INTO placesfollowed(placeid, userid) VALUES (400, '077f3ea6-2272-4457-a47e-9e9111108e44');"""),
      5.seconds)
  }

  val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
  val savedIp = "127.0.0.0"

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
      whenReady(placeMethods.findFollowedPlaces(userUUID), timeout(Span(5, Seconds))) { followedPlaces =>

        followedPlaces.map (_.place.id) should contain (Some(400))
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
        geographicPoint = geographicPointMethods.stringToTryPoint("5.4,5.6").get)
      whenReady(organizerMethods.saveWithAddress(OrganizerWithAddress(organizer, None)), timeout(Span(5, Seconds))) { savedOrganizer =>
        val organizerId = savedOrganizer.organizer.id.get
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>

          whenReady(organizerMethods.followByOrganizerId(UserOrganizerRelation(uuid, organizerId)),
            timeout(Span(5, Seconds))) { response =>

              response mustBe 1

            whenReady(organizerMethods.findFollowedOrganizers(uuid), timeout(Span(5, Seconds))) { organizers =>

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

    "save a guestUser" in {
      whenReady(userMethods.saveGuestUser(GuestUser("127.0.0.1", None)), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }
    }

    "update a guestUser" in {
      whenReady(userMethods.updateGuestUser(GuestUser(
        ip = "127.0.0.7",
        userUuid = Some(UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))
      )), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }
    }

    "maybe link user to guestUser" in {
      val expectedGuestUser = GuestUser(
        ip = "127.0.0.6",
        userUuid = Some(UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))
      )
      whenReady(userMethods.maybeLinkGuestUser("127.0.0.6", UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))) {
        response =>
          response mustEqual expectedGuestUser
      }
    }

    "find a guestUser by ip" in {
      whenReady(userMethods.findGuestUserByIp(savedIp), timeout(Span(5, Seconds))) { response =>
        response mustBe Some(GuestUser("127.0.0.0", None))
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
