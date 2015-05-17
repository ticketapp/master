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

      find(trackId.get) mustEqual Option(track.copy(trackId = trackId, confidence = Some(0)))
      delete(trackId.get) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "be able to be rated up by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

      upsertRatingUp("userTestId", trackId, 1) mustBe Success(true)
      getRatingForUser("userTestId", trackId) mustBe Success(Some("1,0"))

      upsertRatingUp("userTestId", trackId, 2) mustBe Success(true)
      getRatingForUser("userTestId", trackId) mustBe Success(Some("3,0"))

      deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

      "be able to be rated down by a user" in {
        val artistId = Artist.save(artist).get
        val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

        upsertRatingDown("userTestId", trackId, -1) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some("0,1"))

        upsertRatingDown("userTestId", trackId, -2) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some("0,3"))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }

      "be able to be added to favorites and deleted from favorites" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        addToFavorites("userTestId", trackId) mustBe Success(1)
        findFavorites("userTestId") mustBe Success(Seq(track.copy(trackId = Option(trackId), confidence = Some(0))))
        removeFromFavorites("userTestId", trackId) mustBe Success(1)

        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }

      "get rating of a track as a String" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        getRating(trackId) mustBe Success(Some("0,0"))
      }

      "calculate confidence with rating up and down as a String and the new rating" in {
        calculateConfidence("0,0", -1000) mustBe -1000
        calculateConfidence("0,0", 1000) mustBe 997301
        calculateConfidence("500,1200", 0) mustBe 276279
        calculateConfidence("500,1100", -100) mustBe 276279
      }

      "have his confidence updated" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        updateConfidence(trackId, 5000) mustBe Success(1)
        find(trackId) mustEqual Option(track.copy(trackId = Option(trackId), confidence = Some(5000)))

        updateConfidence(trackId, 2000) mustBe Success(1)
        find(trackId) mustEqual Option(track.copy(trackId = Option(trackId), confidence = Some(2000)))

        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }
  }
}
