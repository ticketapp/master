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
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import play.api.libs.concurrent.Execution.Implicits._
import services.Utilities._

class TestSearchArtistController extends PlaySpec with OneAppPerSuite {

  "SearchArtistController" must {

    "find e sequence of artists on facebook" in {
      whenReady (getEventuallyFacebookArtists("rone"), timeout(Span(6, Seconds))) { artists =>
        println(artists.head)
        artists should not be empty
      }
    }

    "find one artist on facebook" in {
      whenReady (getFacebookArtistByFacebookUrl("djvadim"), timeout(Span(6, Seconds))) { artist =>
        println(artist)
        artist shouldBe defined
      }
    }

    "normalize facebook urls" in {
      normalizeFacebookUrl("facebook.com/djvadim") mustBe "djvadim"
      normalizeFacebookUrl("https://www.facebook.com/djvadim?_rdr") mustBe "djvadim"
    }

    "remove useless words in a SoundCloudUrl (even if it contains uppercase letters)" in {
      val refactoredSoundCloudWebsite = removeUselessInSoundCloudWebsite(
        normalizeUrl("https://sounDcloud.com/nina-kraviz/live-at-space-closing-fiesta"))
      refactoredSoundCloudWebsite mustBe "soundcloud.com/nina-kraviz"
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

