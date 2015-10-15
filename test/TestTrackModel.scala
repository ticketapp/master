import java.util.UUID.randomUUID

import models._
import org.postgresql.util.PSQLException
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger

import scala.util.{Failure, Success}

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

    "be saved and deleted" in {
      val trackId = randomUUID
      val track = Track(trackId, "title100", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")

      save(track) mustBe Success(true)
      find(trackId) mustEqual Success(Option(track.copy(uuid = trackId, confidence = Some(0))))
      delete(trackId) mustBe Success(1)
    }

    "not be saved twice for same title and artistName" in {
      val trackId = randomUUID
      val trackId2 = randomUUID
      val track = Track(trackId, "title2", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      val track2 = Track(trackId2, "title2", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")

      try {
        save(track)
        save(track2) match {
          case Failure(psqlException: PSQLException) if psqlException.getSQLState == UNIQUE_VIOLATION =>
          case _ =>
            throw new Exception("save twice a track with same title and artist name worked!")
        }
      } finally {
        delete(trackId)
      }
    }

    "be rated up by a user" in {
      val trackId = randomUUID
      save(Track(trackId, "title4", "url1", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName"))

      try {
        upsertRatingUp("userTestId", trackId, 1) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(1, 0))

        upsertRatingUp("userTestId", trackId, 2) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(3, 0))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      } finally {
        deleteRatingForUser("userTestId", trackId)
        delete(trackId)
      }
    }

    "be rated down by a user" in {
      val trackId = randomUUID
      save(Track(trackId, "title5", "url2", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName"))

      try {
        upsertRatingDown("userTestId", trackId, -1, None) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,1))

        upsertRatingDown("userTestId", trackId, -2, Some('r')) mustBe Success(true)
        getRatingForUser("userTestId", trackId) mustBe Success(Some(0,3))

        deleteRatingForUser("userTestId", trackId) mustBe Success(1)
      } finally {
        deleteRatingForUser("userTestId", trackId)
        delete(trackId)
      }
    }

    "be added to favorites and deleted from favorites" in {
      val trackId = randomUUID
      val track = Track(trackId, "title6", "url3", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)

      try {
        addToFavorites("userTestId", trackId) mustBe Success(1)
        findFavorites("userTestId") mustBe Success(Seq(track.copy(confidence = Some(0))))
        removeFromFavorites("userTestId", trackId) mustBe Success(1)
      } finally {
        delete(trackId)
      }
    }

    "update rating up&down and confidence" in {
      val newTrackId = randomUUID
      val track = Track(newTrackId, "title7", "url5", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)

      try {
        persistUpdateRating(newTrackId, 1, 2, 0.46922029272774324) mustBe Success(1)

        getRating(newTrackId) mustBe Success(Some(1, 2))
        find(newTrackId) mustEqual
          Success(Option(track.copy(confidence = Some(0.46922029272774324))))

        persistUpdateRating(newTrackId, 8, 7, -15)

        getRating(newTrackId) mustBe Success(Some(8, 7))
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(-15))))
      } finally {
        delete(newTrackId)
      }
    }

    "get ratings up and down" in {
      val trackId = randomUUID
      Logger.info("get ratings up and down " + save(Track(trackId, "title9", "url8", 'y', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")).toString)

      try {
        getRating(trackId) mustBe Success(Some((0,0)))

        updateRating(trackId, 5) mustBe Success(calculateConfidence(5,0))
        getRating(trackId) mustBe Success(Some((5,0)))

        updateRating(trackId, -1000) mustBe Success(calculateConfidence(5,1000))
        getRating(trackId) mustBe Success(Some((5,1000)))
      } finally {
        delete(trackId)
      }
    }

    "calculate confidence with rating up and down" in {
      calculateConfidence(0, 15) mustBe -0.015
      calculateConfidence(5000, 0) mustBe 0.6488845039956165
      calculateConfidence(510, 500) mustBe 0.0746875564598663
      calculateConfidence(500, 510) mustBe 0.07199615149144027
      calculateConfidence(5000, 2000) mustBe 0.4086681496298129
    }

    "have his confidence updated" in {
      val newTrackId = randomUUID
      val track = Track(newTrackId, "title10", "url6", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      Logger.info("have his confidence updated " + save(track).toString)

      try {
        var confidence = calculateConfidence(5000, 0)
        updateRating(newTrackId, 5000) mustBe Success(confidence)
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
        getRating(newTrackId) mustBe Success(Some((5000, 0)))

        confidence = calculateConfidence(5000, 2000)
        updateRating(newTrackId, -2000) mustBe Success(confidence)
        find(newTrackId) mustEqual Success(Option(track.copy(confidence = Some(confidence))))
        getRating(newTrackId) mustBe Success(Some((5000, 2000)))
      } finally {
        delete(newTrackId)
      }
    }

    "find all tracks sorted by confidence for an artist" in {
      val artist = Artist(None, None, "artistTest2", None, None, "artistFacebookUrlTestTrack2", Set("website"))
      val artistId = Artist.save(artist).get
      val newTrackId = randomUUID
      val newTrackId2 = randomUUID
      val track = Track(newTrackId, "title11", "url7", 's', "thumbnailUrl", "artistFacebookUrlTestTrack2", "artistName")
      val track2 = Track(newTrackId2, "title12", "url9", 's', "thumbnailUrl", "artistFacebookUrlTestTrack2", "artistName")
      save(track)
      save(track2)

      try {
        updateRating(newTrackId, 5000) mustBe Success(calculateConfidence(5000, 0))

        findAllByArtist(artist.facebookUrl, 0, 0) should contain theSameElementsInOrderAs
          Seq(track.copy(confidence = Some(calculateConfidence(5000, 0))), track2.copy(confidence = Some(0)))
      } finally {
        delete(newTrackId)
        delete(newTrackId2)
        Artist.delete(artistId)
      }
    }

    "find n (numberToReturn) tracks for an artist" in {
      val newTrackId = randomUUID
      val newTrackId2 = randomUUID
      val track = Track(newTrackId, "title13", "url7", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      val track2 = Track(newTrackId2, "title14", "url10", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")
      save(track)
      save(track2)

      try {
        findAllByArtist(artist.facebookUrl, 1, 1) should have length 1
      } finally {
        delete(newTrackId)
        delete(newTrackId2)
      }
    }

    "remove duplicate with same title and artist name without taking account of accentuated letters" in {
      val trackId1 = randomUUID
      val trackId2 = randomUUID
      val tracks = Seq(
        Track(trackId1, "titleNotduplicate", "urlduplicate", 'y', "thumb", "a", "artistNameDuplicate"),
        Track(trackId2, "titleduplicaté", "urlduplicate", 'y', "thumb", "a", "artistNameDuplicate"),
        Track(randomUUID, "titleduplicate", "urlduplicate", 'y', "thumb", "a", "artistNameDuplicate"))

      val expectedTracks = Seq(
        Track(trackId1, "titleNotduplicate", "urlduplicate", 'y', "thumb", "a", "artistNameDuplicate"),
        Track(trackId2, "titleduplicaté", "urlduplicate", 'y', "thumb", "a", "artistNameDuplicate"))
      removeDuplicateByTitleAndArtistName(tracks) must contain theSameElementsAs expectedTracks
    }

    "return true if artist name is in the title and vice-versa without taking account of accentuated letters" in {
      isArtistNameInTrackTitle("brassens trackTitle", "brassens") mustBe true
      isArtistNameInTrackTitle("brassens trackTitle", "Brassens") mustBe true
      isArtistNameInTrackTitle("Brassens trackTitle", "brassens") mustBe true
      isArtistNameInTrackTitle("Bràsséns trackTitle", "brassens") mustBe true
      isArtistNameInTrackTitle("Brâssens trackTitle", "brassêns") mustBe true
      isArtistNameInTrackTitle("Brassens trackTitle", "notRelatedArtist") mustBe false
    }
  }
}
