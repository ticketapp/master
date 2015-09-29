import controllers.SearchArtistsController._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._

import play.api.Play.current

import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities._

class TestSearchArtistController extends PlaySpec with OneAppPerSuite {

  "SearchArtistController" must {

    "find a sequence of artists on Facebook" in {
      whenReady (getEventuallyFacebookArtists("rone"), timeout(Span(6, Seconds))) { artists =>
        artists should not be empty
      }
    }

    "find Rone (an artist) on Facebook" in {
      whenReady (getFacebookArtistByFacebookUrl("rone"), timeout(Span(6, Seconds))) { artist =>
        artist shouldBe defined
      }
    }

    "normalize Facebook urls" in {
      normalizeFacebookUrl("Facebook.com/djvadim") mustBe "djvadim"
      normalizeFacebookUrl("https://www.Facebook.com/djvadim?_rdr") mustBe "djvadim"
    }

    "remove useless words in a SoundCloudUrl (even if it contains uppercase letters)" in {
      val refactoredSoundCloudWebsite1 = removeUselessInSoundCloudWebsite(
        normalizeUrl("https://sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite2 = removeUselessInSoundCloudWebsite(
        normalizeUrl("sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      val refactoredSoundCloudWebsite3 = removeUselessInSoundCloudWebsite(
        normalizeUrl("sounDcloud.com/nina-kraviz"))

      refactoredSoundCloudWebsite1 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite2 mustBe "soundcloud.com/nina-kraviz"
      refactoredSoundCloudWebsite3 mustBe "soundcloud.com/nina-kraviz"
    }

    "remove nothing in a non-soundCloud website" in {
      val refactoredSoundCloudWebsite = removeUselessInSoundCloudWebsite(
        normalizeUrl("https://facebook.com/nina-kraviz/live-at-space-closing-fiesta"))
      refactoredSoundCloudWebsite mustBe "facebook.com/nina-kraviz/live-at-space-closing-fiesta"
    }

    "aggregate the image path url with its offsets" in {
      aggregateImageAndOffset("imageUrl", Option(1), Option(200)) mustBe """imageUrl\1\200"""
      aggregateImageAndOffset("imageUrl", None, Option(200)) mustBe """imageUrl\0\200"""
      aggregateImageAndOffset("imageUrl", Option(1), None) mustBe """imageUrl\1\0"""
      aggregateImageAndOffset("imageUrl", None, None) mustBe """imageUrl\0\0"""
    }
  }
}

