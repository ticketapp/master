import models.Artist
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import services.SearchSoundCloudTracks._
import play.api.libs.json.{JsValue, Json}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._

class TestSearchSoundCloudTracks extends PlaySpec with OneAppPerSuite {

  val ninaKraviz = Artist(None, Option("facebookId3"), "nina", Option("imagePath"), Option("description"),
    "facebookUrl3", Set("soundcloud.com/nina-kraviz"))
  val worakls = Artist(None, Option("facebookId3"), "worakls", Option("imagePath"), Option("description"),
    "worakls", Set.empty)

  "SearchSoundCloudTracks" must {

    "find tracks on SoundCloud" in {
      whenReady(getSoundCloudTracksForArtist(ninaKraviz), timeout(Span(3, Seconds))) { tracks =>
        tracks should not be empty
      }
    }

    "find soundCloud ids for artist name worakls" in {
      whenReady(getSoundCloudIdsForName("worakls"), timeout(Span(2, Seconds))) { soundCloudIds =>
        soundCloudIds should contain allOf (68442, 4329372, 13302835)
      }
    }

    "find soundCloud websites for a list of soundCloud ids" in {
      whenReady(getTupleIdAndSoundCloudWebsitesForIds(List(68442, 4329372, 13302835, 97091845, 129311935, 366396)),
        timeout(Span(2, Seconds))) { tupleSoundCloudIdWebsites =>
        tupleSoundCloudIdWebsites should contain
        (68442, Seq("hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls", "facebook.com/worakls"))
      }
    }
  }
}
