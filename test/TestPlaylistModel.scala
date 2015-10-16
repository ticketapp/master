import java.util.UUID.randomUUID

import models.Playlist._
import models.{Track, _}
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

import scala.util.Success

class TestPlaylistModel extends PlaySpec with BeforeAndAfterAll with OneAppPerSuite {
  /*var artistId = -1L
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

    "be able to be saved with its tracks, rendered sorted by rank and deleted" in {
      val trackId1 = randomUUID
      val track1 = Track(trackId1, "title", "urlPlaylistTest", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None)
      val trackId2 = randomUUID
      val track2 = Track(trackId2, "title2", "urlPlaylistTest2", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, Some(0), Some(1))
      val trackId3 = randomUUID
      val track3 = Track(trackId3, "title3", "urlPlaylistTest3", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, Some(0), Some(2))
      Track.save(track1)
      Track.save(track2)
      Track.save(track3)

      try {

        val playlistId = Playlist.saveWithTrackRelation("userTestId",
          PlaylistNameTracksIdAndRank("name",
            Seq(TrackUUIDAndRank(trackId1, 0), TrackUUIDAndRank(trackId2, 2), TrackUUIDAndRank(trackId3, 1))))

        Track.findByPlaylistId(Some(playlistId)) mustBe Seq(
          track1.copy(confidence = Some(0), playlistRank =  Some(0)),
          track3.copy(confidence = Some(0), playlistRank =  Some(1)),
          track2.copy(confidence = Some(0), playlistRank =  Some(2)))

        Playlist.delete("userTestId", playlistId) mustBe Success(1)

      } finally {
        Track.delete(trackId1)
        Track.delete(trackId2)
        Track.delete(trackId3)
      }
    }
  }*/
}
