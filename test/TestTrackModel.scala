import java.util.UUID
import java.util.UUID.randomUUID

import models._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}

import scala.language.postfixOps
import scala.util.Success


class TestTrackModel extends GlobalApplicationForModels {
  var artistId = -1L
  val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, Option("facebookIdTestTrack"), "artistTest", Option("imagePath"),
    Option("description"), "artistFacebookUrlTestTrack", Set("website")), Vector.empty)

  "A track" must {

    "be found by playlist id" in {
      whenReady(trackMethods.findByPlaylistId(1L), timeout(Span(5, Seconds))) { tracksWithPlaylistRank =>
        assert(isOrdered((tracksWithPlaylistRank map (_.rank)).toList))
      }
    }

    "be saved and deleted" in {
      val trackId = UUID.randomUUID
      val track = Track(trackId, "title100", "url", 's', "thumbnailUrl", "facebookUrl0", "artistName")
      whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
        savedTrack.uuid mustBe trackId
        whenReady(trackMethods.find(trackId), timeout(Span(5, Seconds))) { trackFound =>
          trackFound mustEqual Option(track.copy(uuid = trackId, confidence = 0))

          whenReady(trackMethods.delete(trackId), timeout(Span(5, Seconds))) {
            _ mustBe 1
          }
        }
      }
    }

    "not be saved twice for same title and artistName" in {
      val trackId = UUID.randomUUID
      val trackId2 = UUID.randomUUID
      val track = Track(uuid = trackId, title = "title2", url = "url", 's', "thumbnailUrl", "facebookUrl0", "artistName")
      val track2 = Track(uuid = trackId2, title = "title2", url = "url1", 's', "thumbnailUrl", "facebookUrl0", "artistName")

      whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
        whenReady(trackMethods.save(track2), timeout(Span(5, Seconds))) {
            case track1 if track1 == savedTrack =>
            case _ =>
              throw new Exception("save twice a track with same title and artist name worked!")
        }
      }
    }

    "be found by pattern" in {
      whenReady(trackMethods.findAllContainingInTitle("title0"), timeout(Span(5, Seconds))) { tracks =>
        val titles = tracks map(_.title)
        titles should contain allOf("title0", "title00", "title000")
        titles should not contain "title"
      }
    }

    "be found by genre" in {
      whenReady(trackMethods.findAllByGenre(genreName = "genreTest0", numberToReturn = 10, offset = 0),
        timeout(Span(5, Seconds))) { tracks =>
        val titles = tracks map(_.title)

        titles should contain only "title0"
      }
    }

    "be rated up or down by a user" in {
      val userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
      val trackId = UUID.fromString("02894e56-08d1-4c1f-b3e4-466c069d15ed")

      whenReady(trackRatingMethods.upsertRatingForAUser(userId = userId, trackId = trackId, rating = 1),
        timeout(Span(5, Seconds))) { response =>

        response mustBe 1
      }

      whenReady(trackRatingMethods.getRatingForUser(userId, trackId), timeout(Span(5, Seconds))) { rating =>
        rating mustBe Option(Rating(ratingUp = 1, ratingDown = 0))
      }

      whenReady(trackRatingMethods.upsertRatingForAUser(userId, trackId, -2), timeout(Span(5, Seconds))) { response =>
        response mustBe 1
      }

      whenReady(trackRatingMethods.getRatingForUser(userId, trackId), timeout(Span(5, Seconds))) { response =>
        response mustBe Option(Rating(ratingUp = 1, ratingDown = -2))
      }

        whenReady(trackRatingMethods.deleteRatingForUser(userId, trackId), timeout(Span(5, Seconds))) { response =>
          response mustBe 1
        }
    }

//    "be added to favorites and deleted from favorites" in {
//      trackRatingMethods.addToFavorites("userTestId", trackId) mustBe Success(1)
//      findFavorites("userTestId") mustBe Success(Seq(track.copy(confidence = Some(0))))
//      removeFromFavorites("userTestId", trackId) mustBe Success(1)
//    }

    "update rating up&down and confidence" in {
      whenReady(trackRatingMethods.persistUpdateRating(UUID.fromString("13894e56-08d1-4c1f-b3e4-466c069d15ed"), 1, 2,
        0.46922029272774324), timeout(Span(5, Seconds))) { result =>
        result mustBe 1
      }
    }

    "get ratings up and down" in {
      val trackId = UUID.fromString("24894e56-08d1-4c1f-b3e4-466c069d15ed")

      whenReady(trackRatingMethods.getGeneralRating(trackId), timeout(Span(5, Seconds))) { result =>
        result mustBe Some(Rating(0, 0))
      }

      whenReady(trackRatingMethods.updateGeneralRating(trackId, 5), timeout(Span(5, Seconds))) { result =>
        result.get mustBe trackMethods.calculateConfidence(5, 0)
      }

      whenReady(trackRatingMethods.getGeneralRating(trackId), timeout(Span(5, Seconds))) { result =>
        result mustBe Some(Rating(5, 0))
      }

      whenReady(trackRatingMethods.updateGeneralRating(trackId, -1000), timeout(Span(5, Seconds))) { result =>
        result.get mustBe trackMethods.calculateConfidence(5, 1000)
      }

      whenReady(trackRatingMethods.getGeneralRating(trackId), timeout(Span(5, Seconds))) { result =>
        result mustBe Some(Rating(5, 1000))
      }
    }

    "calculate confidence with rating up and down" in {
      trackMethods.calculateConfidence(0, 15) mustBe -0.015
      trackMethods.calculateConfidence(5000, 0) mustBe 0.6488845039956165
      trackMethods.calculateConfidence(510, 500) mustBe 0.0746875564598663
      trackMethods.calculateConfidence(500, 510) mustBe 0.07199615149144027
      trackMethods.calculateConfidence(5000, 2000) mustBe 0.4086681496298129
    }

    "have his confidence updated" in {
      val trackId = UUID.fromString("35894e56-08d1-4c1f-b3e4-466c069d15ed")
      var confidence = trackMethods.calculateConfidence(5000, 0)

      whenReady(trackRatingMethods.updateGeneralRating(trackId, 5000), timeout(Span(5, Seconds))) { result =>
        result mustBe Success(confidence)
      }

      whenReady(trackMethods.find(trackId), timeout(Span(5, Seconds))) { result =>
        result.get mustEqual result.get.copy(confidence = confidence)
      }

      whenReady(trackRatingMethods.getGeneralRating(trackId), timeout(Span(5, Seconds))) { result =>
        result mustBe Some(Rating(5000, 0))
      }

      confidence = trackMethods.calculateConfidence(5000, 2000)

      whenReady(trackRatingMethods.updateGeneralRating(trackId, -2000), timeout(Span(5, Seconds))) { result =>
        result.get mustBe trackMethods.calculateConfidence(5000, 2000)
      }

      whenReady(trackMethods.find(trackId), timeout(Span(5, Seconds))) { result =>
        result.get mustEqual result.get.copy(confidence = confidence)
      }
    }

    "find all tracks sorted by confidence for an artist" in {
      whenReady(trackMethods.findAllByArtist("facebookUrl00", 0, 0)) { tracks =>
        tracks map (_.confidence) should contain theSameElementsInOrderAs Seq(0.4086681496298129, 9.160612963143905E-6)
      }
    }

    "find n (numberToReturn) tracks for an artist" in {
      whenReady(trackMethods.findAllByArtist("facebookUrl0", 1, 1), timeout(Span(5, Seconds))) { tracksSeq =>
        tracksSeq.length mustBe 1
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
      trackMethods.removeDuplicateByTitleAndArtistName(tracks) must contain theSameElementsAs expectedTracks
    }

    "return true if artist name is in the title and vice-versa without taking account of accentuated letters" in {
      trackMethods.isArtistNameInTrackTitle("brassens trackTitle", "brassens") mustBe true
      trackMethods.isArtistNameInTrackTitle("brassens trackTitle", "Brassens") mustBe true
      trackMethods.isArtistNameInTrackTitle("Brassens trackTitle", "brassens") mustBe true
      trackMethods.isArtistNameInTrackTitle("Bràsséns trackTitle", "brassens") mustBe true
      trackMethods.isArtistNameInTrackTitle("Brâssens trackTitle", "brassêns") mustBe true
      trackMethods.isArtistNameInTrackTitle("Brassens trackTitle", "notRelatedArtist") mustBe false
    }
  }
}
