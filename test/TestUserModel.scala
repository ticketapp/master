import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import silhouette.UserDAOImpl

class TestUserModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

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
        }
      }
    }

//    "get his followed places" in {
//      val place = Place(None, "test", None, None, None, None, None, None, None)
//
//      whenReady(Place.save(place), timeout(Span(2, Seconds))) { tryPlaceId =>
//        val placeId = tryPlaceId.get.get
//        try {
//          Place.followByPlaceId("userTestId", placeId)
//
//          Place.getFollowedPlaces(IdentityId("userTestId", "providerId")) should not be empty
//
//          Place.unfollowByPlaceId("userTestId", placeId) mustBe Success(1)
//        } finally {
//          Place.delete(placeId)
//        }
//      }
//    }
//
//    "get his followed organizers" in {
//      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
//        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
//        geographicPoint = Option("(5.4,5.6)"))
//      val organizerId = Organizer.save(organizer).get.get
//      try {
//        Organizer.followByOrganizerId("userTestId", organizerId)
//
//        Organizer.getFollowedOrganizers(IdentityId("userTestId", "providerId")) should not be empty
//
//        Organizer.unfollowByOrganizerId("userTestId", organizerId) mustBe Success(1)
//      } finally {
//        Organizer.delete(organizerId)
//      }
//    }
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
