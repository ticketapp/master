import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models._
import models.Playlist._
import models.Track._
import securesocial.core.IdentityId
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import java.util.UUID.randomUUID

import scala.util.Success

class TestPlaylistModel extends PlaySpec with OneAppPerSuite {

  "A playlist" must {

    "be able to be saved and deleted" in {
      val playlist = Playlist(None, "userTestId", "name", Seq.empty)
      val playlistId = Playlist.save(playlist) match {
        case Success(Some(long)) => long
        case _ => throw new Exception("playlist not saved")
      }

      Playlist.delete("userTestId", playlistId) mustBe Success(false)
    }

    "be able to be saved with its tracks" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest", Option("imagePath"),
        Option("description"), "artistFacebookUrl", Set("website"))
      val artistId = Artist.save(artist).get
      val trackId = randomUUID.toString
      val track = Track(trackId, "title", "url", 's', "thumbnailUrl", "artistFacebookUrl", "artistName")

      Track.save(track) mustBe Success(true)


      val playlist = Playlist(None, "userTestId", "name", Seq.empty)
      val playlistId = Playlist.saveWithTrackRelation("userTestId",
        PlaylistNameTracksIdAndRank("name", Seq(TrackIdAndRank(trackId, 0))))

      Playlist.delete("userTestId", playlistId) mustBe Success(false)
      Track.delete(trackId) mustBe 1
      Artist.delete(artistId) mustBe 1
    }
  }
}
