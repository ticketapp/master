import controllers.SearchArtistsController._
import org.postgresql.util.PSQLException
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

  }
}

