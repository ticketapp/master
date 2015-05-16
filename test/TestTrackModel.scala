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
      val trackId = save(track).get

      find(trackId.get) mustEqual Option(track.copy(trackId = trackId))
      delete(trackId.get) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "be able to be rated up by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

      upsertRatingUp("userTestId", trackId, 1) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some("1,0"))

      upsertRatingUp("userTestId", trackId, 2) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some("3,0"))

      deleteRating("userTestId", trackId) mustBe Success(1)
      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }  
    
    "be able to be rated down by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

      upsertRatingDown("userTestId", trackId, -1) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some("0,-1"))

      upsertRatingDown("userTestId", trackId, -2) mustBe Success(true)
      getRating("userTestId", trackId) mustBe Success(Some("0,-3"))

      deleteRating("userTestId", trackId) mustBe Success(1)
      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "be able to be added to favorites and deleted from favorites" in {
      val artistId = Artist.save(artist).get
      val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
      val trackId = save(track).get.get

      addToFavorites("userTestId", trackId) mustBe Success(1)
      findFavorites("userTestId") mustBe Success(Seq(track.copy(trackId = Option(trackId))))
      removeFromFavorites("userTestId", trackId) mustBe Success(1)

      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }
  }
}
