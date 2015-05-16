import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Track._
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._

import scala.util.Success

class TestTrackModel extends PlaySpec with OneAppPerSuite {

  "A track" must {
    val artist = Artist(None, Option("facebookId3"), "artistTest", Option("imagePath"),
      Option("description"), "artistFacebookUrl", Set("website"))

    "be able to be saved and deleted" in {
      val artistId = Artist.save(artist).get
      val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
      val trackId = save(track)

      find(trackId.get) mustEqual Option(track.copy(trackId = trackId))
      delete(trackId.get) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "be able to be noted by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get

      upsertRating("userTestId", trackId, -2) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some(-2))

      upsertRating("userTestId", trackId, 2) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some(0))

      deleteRating("userTestId", trackId) mustBe Success(1)
      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }
  }
}
