import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Playlist._
import models.Track
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import java.util.UUID.randomUUID

import scala.util.Success

class TestPlaylistModel extends PlaySpec with BeforeAndAfterAll with OneAppPerSuite {
  var artistId = -1L
  val artist = Artist(None, Option("facebookIdTestTrack"), "artistTest", Option("imagePath"),
    Option("description"), "artistFacebookUrlTestPlaylistModel", Set("website"))

  override def beforeAll() = {
    artistId = Artist.save(artist).get
  }

  override def afterAll() = {
    Artist.delete(artistId)
  }

  "A playlist" must {

    "be able to be saved and deleted" in {
      val playlist = Playlist(None, "userTestId", "name", Seq.empty)
      val playlistId = Playlist.save(playlist) match {
        case Success(Some(newPlaylistId)) =>
          find(newPlaylistId) mustBe Option(playlist.copy(Some(newPlaylistId)))
          newPlaylistId
        case _ =>
          throw new Exception("playlist not saved")
      }
      Playlist.delete("userTestId", playlistId) mustBe Success(1)
    }

    "be able to be saved with its tracks and deleted" in {
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "urlPlaylistTest", 's', "thumbnailUrl", "artistFacebookUrlTestPlaylistModel", "name")
      val trackId2 = randomUUID.toString
      val track2 = Track(trackId2, "title2", "urlPlaylistTest2", 's', "thumbnailUrl", "artistFacebookUrlTestPlaylistModel", "name")
      val a = Track.save(track)
      val b = Track.save(track2)

      try {

        val playlistId = Playlist.saveWithTrackRelation("userTestId",
          PlaylistNameTracksIdAndRank("name", Seq(TrackIdAndRank(trackId, 0), TrackIdAndRank(trackId2, 1))))

        Track.findByPlaylistId(Some(playlistId)) should have length 2
        Playlist.delete("userTestId", playlistId) mustBe Success(1)

      } catch {
        case e: Exception => throw e
      } finally {
        Track.delete(trackId)
        Track.delete(trackId2)
      }
    }
  }
}
