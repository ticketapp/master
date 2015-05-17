import models.Artist
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import services.SearchSoundCloudTracks._
import play.api.libs.json.{JsValue, Json}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._

class TestSearchSoundCloudTracks extends PlaySpec with OneAppPerSuite {

  val artist = Artist(None, Option("facebookId3"), "nina", Option("imagePath"), Option("description"),
    "facebookUrl3", Set("soundcloud.com/nina-kraviz"))

  "SearchSoundCloudTracks" must {

    "find tracks on SoundCloud" in {
      whenReady(getSoundCloudTracksForArtist(artist), timeout(Span(2, Seconds))) { tracks =>
        tracks should not be empty
      }
    }
  }
}