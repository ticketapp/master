import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.Enumerator
import scala.concurrent.Future
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import play.api.libs.iteratee.Input.EOF
import play.api.libs.ws.{WS, Response}
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import scala.concurrent.Future
import services.SearchSoundCloudTracks.normalizeTrackTitle
import models.Genre.saveGenreForArtistInFuture
import scala.language.postfixOps
import services.Utilities._

import scala.util.{Success, Failure, Try}

class TestTest extends PlaySpec {

  "This test" must {
    val a = Enumerator(1, 2, 3)
//    println(a.
  }
}