import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{User, Organizer, Place}
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import scala.util.Success
import java.util.Date

class TestUserModel extends PlaySpec with OneAppPerSuite {

  "A user" must {

    "be saved and deleted" in {
//      val user = User("userTestId", new Date(), "email", "nickName", "password", "profile")
//      save()
    }

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
