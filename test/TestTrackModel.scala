import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Track._
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import java.util.UUID.randomUUID

import scala.util.Success

class TestTrackModel extends PlaySpec with OneAppPerSuite {

  "A track" must {
    val artist = Artist(None, Option("facebookId3"), "artistTest", Option("imagePath"),
      Option("description"), "artistFacebookUrl", Set("website"))

    "be able to be saved and deleted" in {
      val artistId = Artist.save(artist).get
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "url", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")

      save(track) mustBe Success(true)
      find(trackId) mustEqual Success(Option(track.copy(trackId = trackId, confidence = Some(0))))
      delete(trackId) mustBe 1

      Artist.delete(artistId) mustBe 1
    }

    "be able to be rated up by a user" in {
      val artistId = Artist.save(artist).get
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url1", 'y', "thumbnailUrl", "artistFacebookUrl",
        "artistName")) mustBe Success(true)

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
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url2", 's', "thumbnailUrl", "artistFacebookUrl",
        "artistName")) mustBe Success(true)

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
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "url3", 'y', "thumbnailUrl", "artistFacebookUrl", "artistName")
      save(track) mustBe Success(true)

      addToFavorites("userTestId", trackId) mustBe Success(1)
      findFavorites("userTestId") mustBe Success(Seq(track.copy(confidence = Some(0))))
      removeFromFavorites("userTestId", trackId) mustBe Success(1)

      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "update rating up&down and confidence" in {
      val artistId = Artist.save(artist).get
      val newTrackId = randomUUID.toString
      val track = Track(newTrackId, "title", "url5", 'y', "thumbnailUrl", "artistFacebookUrl", "artistName")
      save(track) mustBe Success(true)

      persistUpdateRating(newTrackId, 1, 2, 0.46922029272774324) mustBe Success(1)

      getRating(newTrackId) mustBe Success(Some(1, 2))
      find(newTrackId) mustEqual
        Success(Option(track.copy(confidence = Some(0.46922029272774324))))

      persistUpdateRating(newTrackId, 8, 7, -15)

      getRating(newTrackId) mustBe Success(Some(8, 7))
      find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(-15))))

      delete(newTrackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "get ratings up and down" in {
      val artistId = Artist.save(artist).get
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url4", 'y', "thumbnailUrl", "artistFacebookUrl", "artistName")) mustBe
        Success(true)

      getRating(trackId) mustBe Success(Some((0,0)))

      updateRating(trackId, 5) mustBe Success(calculateConfidence(5,0))
      getRating(trackId) mustBe Success(Some((5,0)))

      updateRating(trackId, -1000) mustBe Success(calculateConfidence(5,1000))
      getRating(trackId) mustBe Success(Some((5,1000)))

      delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "calculate confidence with rating up and down" in {
      calculateConfidence(0, 15) mustBe -15
      calculateConfidence(5000, 0) mustBe 0.9994591863331846
      calculateConfidence(510, 500) mustBe 0.4790948314645526
      calculateConfidence(500, 510) mustBe 0.46922029272774324
      calculateConfidence(5000, 2000) mustBe 0.7053228985989436
    }

    "have his confidence updated" in {
      val artistId = Artist.save(artist).get
      val newTrackId = randomUUID.toString
      val track = Track(newTrackId, "title", "url6", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")
      save(track) mustBe Success(true)

      var confidence = calculateConfidence(5000, 0)
      updateRating(newTrackId, 5000) mustBe Success(confidence)
      find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
      getRating(newTrackId) mustBe Success(Some((5000, 0)))

      confidence = calculateConfidence(5000, 2000)
      updateRating(newTrackId, -2000) mustBe Success(confidence)
      find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
      getRating(newTrackId) mustBe Success(Some((5000, 2000)))

      delete(newTrackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "find all tracks sorted by confidence for an artist" in {
      val artistId = Artist.save(artist).get
      val newTrackId = randomUUID.toString
      val newTrackId2 = randomUUID.toString
      val track = Track(newTrackId, "title", "url7", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")
      val track2 = Track(newTrackId2, "title", "url8", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")
      save(track) mustBe Success(true)
      save(track2) mustBe Success(true)

      updateRating(newTrackId, 5000) mustBe Success(calculateConfidence(5000, 0))

      findAllByArtist(artist.facebookUrl, 0, 0) should contain theSameElementsInOrderAs
        Seq(track.copy(confidence = Some(calculateConfidence(5000, 0))), track2.copy(confidence = Some(0)))

      delete(newTrackId) mustBe 1
      delete(newTrackId2) mustBe 1
      Artist.delete(artistId) mustBe 1
    }

    "find n (numberToReturn) tracks for an artist" in {
      val artistId = Artist.save(artist).get
      val newTrackId = randomUUID.toString
      val newTrackId2 = randomUUID.toString
      val track = Track(newTrackId, "title", "url7", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")
      val track2 = Track(newTrackId2, "title", "url8", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")
      save(track) mustBe Success(true)
      save(track2) mustBe Success(true)

      findAllByArtist(artist.facebookUrl, 1, 1) should have length 1

      delete(newTrackId) mustBe 1
      delete(newTrackId2) mustBe 1
      Artist.delete(artistId) mustBe 1
    }
  }
}
