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
      val track = Track(None, "title4", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
      val trackId = save(track).get

      find(trackId.get) mustEqual Option(track.copy(trackId = trackId, confidence = Some(0)))
      delete(trackId.get) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "be able to be rated up by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = save(Track(None, "title5", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

      upsertRatingUp("userTestId", trackId, 1) mustBe Success(true)
      getRatingForUser("userTestId", trackId) mustBe Success(Some(1,0))

      upsertRatingUp("userTestId", trackId, 2) mustBe Success(true)
      getRatingForUser("userTestId", trackId) mustBe Success(Some(3,0))

      deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

      "be able to be rated down by a user" in {
        val artistId = Artist.save(artist).get
        val trackId = save(Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")).get.get

        upsertRatingDown("userTestId", trackId, -1) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,1))

        upsertRatingDown("userTestId", trackId, -2) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,3))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }

      "be able to be added to favorites and deleted from favorites" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title2", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        addToFavorites("userTestId", trackId) mustBe Success(1)
        findFavorites("userTestId") mustBe Success(Seq(track.copy(trackId = Option(trackId), confidence = Some(0))))
        removeFromFavorites("userTestId", trackId) mustBe Success(1)

        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }

      "get ratings up and down" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title3", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        getRating(trackId) mustBe Success(Some((0,0)))

        updateRating(trackId, 5) mustBe Success(calculateConfidence(5,0))
        getRating(trackId) mustBe Success(Some((5,0)))

        updateRating(trackId, -1000) mustBe Success(calculateConfidence(5,1000))
        getRating(trackId) mustBe Success(Some((5,1000)))

        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }
/*
      "calculate confidence with rating up and down" in {
        calculateConfidence(0, 15) mustBe -15
        calculateConfidence(5000, 0) mustBe 0.9994591863331846
        calculateConfidence(510, 500) mustBe 0.4790948314645526
        calculateConfidence(500, 510) mustBe 0.46922029272774324
        calculateConfidence(5000, 2000) mustBe 0.7053228985989436
      }

      "update rating up&down, confidence" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val newTrackId = save(track).get.get

        persistUpdateRating(newTrackId, 1, 2, 0.46922029272774324)

        find(newTrackId) mustEqual
          Option(track.copy(trackId = Option(newTrackId), confidence = Some(0.46922029272774324)))
        getRating(newTrackId) mustBe Success(Some(1, 2))

        persistUpdateRating(newTrackId, 8, 7, -15)

        find(newTrackId) mustEqual Option(track.copy(trackId = Option(newTrackId), confidence = Some(-15)))
        getRating(newTrackId) mustBe Success(Some(8, 7))

        delete(newTrackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }


      "have his confidence updated" in {
        val artistId = Artist.save(artist).get
        val track = Track(None, "title", "url", 'y', "thumbnailUrl", "artistFacebookUrl")
        val trackId = save(track).get.get

        var confidence = calculateConfidence(5000, 0)
        val up = updateRating(trackId, 5000)
        println("up" + up)
        up mustBe Success(confidence)
        find(trackId) mustEqual Option(track.copy(trackId = Option(trackId), confidence = Some(confidence)))
        getRating(trackId) mustBe Success(Some((5000, 0)))

        confidence = calculateConfidence(5000, 2000)
        updateRating(trackId, -2000) mustBe Success(confidence)
        find(trackId) mustEqual Option(track.copy(trackId = Option(trackId), confidence = Some(confidence)))
        getRating(trackId) mustBe Success(Some((5000, 2000)))

        delete(trackId) mustBe 1
        Artist.delete(artistId) mustBe 1
      }*/
  }
}
