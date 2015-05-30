import models.{Organizer, Place}
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import securesocial.core.IdentityId

import scala.util.Success


class TestUserModel extends PlaySpec with OneAppPerSuite {

  "A user" must {

    "be saved and deleted" in {
//      val user = User("userTestId", new Date(), "email", "nickName", "password", "profile")
//      save()
    }

    "be able to get his followed places" in {
      val place = Place(None, "test", None, None, None, None, None, None, None)

      whenReady(Place.save(place), timeout(Span(2, Seconds))) { tryPlaceId =>
        val placeId = tryPlaceId.get.get
        try {
          Place.followByPlaceId("userTestId", placeId)

          Place.getFollowedPlaces(IdentityId("userTestId", "providerId")) should not be empty

          Place.unfollowByPlaceId("userTestId", placeId) mustBe Success(1)
        } finally {
          Place.delete(placeId)
        }
      }
    }

    "be able to get his followed organizers" in {
      val organizer = Organizer(None, Option("facebookId2"), "organizerTest2", Option("description"), None,
        None, Option("publicTransit"), Option("websites"), imagePath = Option("imagePath"),
        geographicPoint = Option("(5.4,5.6)"))
      val organizerId = Organizer.save(organizer).get.get
      try {
        Organizer.followByOrganizerId("userTestId", organizerId)

        Organizer.getFollowedOrganizers(IdentityId("userTestId", "providerId")) should not be empty

        Organizer.unfollowByOrganizerId("userTestId", organizerId) mustBe Success(1)
      } finally {
        Organizer.delete(organizerId)
      }
    }
  }
}
