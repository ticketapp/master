import org.postgresql.util.PSQLException
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Track._
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import java.util.UUID.randomUUID
import services.Utilities.UNIQUE_VIOLATION

import scala.util.{Success, Failure}

class TestTrackModel extends PlaySpec with BeforeAndAfterAll with OneAppPerSuite {
  var artistId = -1L
  val artist = Artist(None, Option("facebookIdTestTrack"), "artistTest", Option("imagePath"),
    Option("description"), "artistFacebookUrlTestTrack", Set("website"))

  override def beforeAll() = {
    artistId = Artist.save(artist).get
  }

  override def afterAll() = {
    Artist.delete(artistId)
  }

  "A track" must {

    "be able to be saved and deleted" in {
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")

      save(track) mustBe Success(true)
      find(trackId) mustEqual Success(Option(track.copy(trackId = trackId, confidence = Some(0))))
      delete(trackId) mustBe Success(1)
    }

    "not be saved twice for same title and artistName" in {
      val trackId = randomUUID.toString
      val trackId2 = randomUUID.toString
      val track = Track(trackId, "title", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      val track2 = Track(trackId2, "title", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")

      save(track)
      save(track2) match {
        case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
        case _ =>
          throw new Exception("save twice a track with same title and artist name worked!")
      }

      delete(trackId)
    }

    "be able to be rated up by a user" in {
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url1", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName"))

      try {
        upsertRatingUp("userTestId", trackId, 1) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(1, 0))

        upsertRatingUp("userTestId", trackId, 2) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(3, 0))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      } catch {
        case e: Exception => throw e
      } finally {
        delete(trackId)
      }
    }

    "be able to be rated down by a user" in {
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url2", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName"))

      try {
        upsertRatingDown("userTestId", trackId, -1) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,1))

        upsertRatingDown("userTestId", trackId, -2) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,3))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      } catch {
        case e: Exception => throw e
      } finally {
        delete(trackId)
      }
    }

    "be able to be added to favorites and deleted from favorites" in {
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "url3", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)

      try {
        addToFavorites("userTestId", trackId) mustBe Success(1)
        findFavorites("userTestId") mustBe Success(Seq(track.copy(confidence = Some(0))))
        removeFromFavorites("userTestId", trackId) mustBe Success(1)
      } catch {
        case e: Exception => throw e
      } finally {
        delete(trackId)
      }
    }

    "update rating up&down and confidence" in {
      val newTrackId = randomUUID.toString
      val track = Track(newTrackId, "title", "url5", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)

      try {
        persistUpdateRating(newTrackId, 1, 2, 0.46922029272774324) mustBe Success(1)

        getRating(newTrackId) mustBe Success(Some(1, 2))
        find(newTrackId) mustEqual
          Success(Option(track.copy(confidence = Some(0.46922029272774324))))

        persistUpdateRating(newTrackId, 8, 7, -15)

        getRating(newTrackId) mustBe Success(Some(8, 7))
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(-15))))
      } catch {
        case e: Exception => throw e
      } finally {
        delete(newTrackId)
      }
    }

    "get ratings up and down" in {
      val trackId = randomUUID.toString
      save(Track(trackId, "title", "url4", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName"))

      try {
        getRating(trackId) mustBe Success(Some((0,0)))

        updateRating(trackId, 5) mustBe Success(calculateConfidence(5,0))
        getRating(trackId) mustBe Success(Some((5,0)))

        updateRating(trackId, -1000) mustBe Success(calculateConfidence(5,1000))
        getRating(trackId) mustBe Success(Some((5,1000)))
      } catch {
        case e: Exception => throw e
      } finally {
        delete(trackId)
      }
    }

    "calculate confidence with rating up and down" in {
      calculateConfidence(0, 15) mustBe -15
      calculateConfidence(5000, 0) mustBe 0.9994591863331846
      calculateConfidence(510, 500) mustBe 0.4790948314645526
      calculateConfidence(500, 510) mustBe 0.46922029272774324
      calculateConfidence(5000, 2000) mustBe 0.7053228985989436
    }

    "have his confidence updated" in {
      val newTrackId = randomUUID.toString
      val track = Track(newTrackId, "title", "url6", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)

      try {
        var confidence = calculateConfidence(5000, 0)
        updateRating(newTrackId, 5000) mustBe Success(confidence)
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
        getRating(newTrackId) mustBe Success(Some((5000, 0)))

        confidence = calculateConfidence(5000, 2000)
        updateRating(newTrackId, -2000) mustBe Success(confidence)
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
        getRating(newTrackId) mustBe Success(Some((5000, 2000)))
      } catch {
        case e: Exception => throw e
      } finally {
        delete(newTrackId)
      }
    }

    "find all tracks sorted by confidence for an artist" in {
      val newTrackId = randomUUID.toString
      val newTrackId2 = randomUUID.toString
      val track = Track(newTrackId, "title", "url7", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      val track2 = Track(newTrackId2, "title2", "url8", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)
      save(track2)

      try {
        updateRating(newTrackId, 5000) mustBe Success(calculateConfidence(5000, 0))

        findAllByArtist(artist.facebookUrl, 0, 0) should contain theSameElementsInOrderAs
          Seq(track.copy(confidence = Some(calculateConfidence(5000, 0))), track2.copy(confidence = Some(0)))
      } catch {
        case e: Exception => throw e
      } finally {
        delete(newTrackId)
        delete(newTrackId2)
      }
    }

    "find n (numberToReturn) tracks for an artist" in {
      val newTrackId = randomUUID.toString
      val newTrackId2 = randomUUID.toString
      val track = Track(newTrackId, "title", "url7", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      val track2 = Track(newTrackId2, "title2", "url8", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)
      save(track2)

      try {
        findAllByArtist(artist.facebookUrl, 1, 1) should have length 1
      } catch {
        case e: Exception => throw e
      } finally {
        delete(newTrackId)
        delete(newTrackId2)
      }
    }
  }
}
