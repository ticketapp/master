import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.Enumerator
import scala.collection.mutable.ListBuffer
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

    "test" in {
      case class A(a: String, b: Int, c: Int)
      var l: List[A] = List(A("j", 1, 2), A("k", 2, 3), A("k", 1, 2), A("k", 2, 3))

      var i = new ListBuffer[(Int, Int)]()
      val j = for {
        t <- l
        if !i.contains((t.b, t.c))
      } yield {
          i += ((t.b, t.c))
          t
        }
      println(j)
    }
  }
}