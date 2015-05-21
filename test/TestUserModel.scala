import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{Organizer, Place}
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import scala.util.Success

class TestUserModel extends PlaySpec with OneAppPerSuite {

  "A user" must {

    "be able to get his followed places" in {
      Place.followByPlaceId("userTestId", 1)

      Place.getFollowedPlaces(IdentityId("userTestId", "providerId")) should not be empty

      Place.unfollowByPlaceId("userTestId", 1) mustBe Success(1)
    }

    "be able to get his followed organizers" in {
      Organizer.followByOrganizerId("userTestId", 1)

      Organizer.getFollowedOrganizers(IdentityId("userTestId", "providerId")) should not be empty

      Organizer.unfollowByOrganizerId("userTestId", 1) mustBe Success(1)
    }
  }
}
