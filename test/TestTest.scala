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

    def getSongsAfter3Second(string: String): Future[String] = Future {
      Thread.sleep(1000)
      string
    }

    def futuresToEnumerator(): Enumerator[String] = {
      Enumerator.flatten(
        getSongsAfter3Second("a") map { Enumerator(_) >>>
          Enumerator.flatten(getSongsAfter3Second("3") map { Enumerator(_) } ) >>>
          Enumerator.eof
        }
      )
    }

    def eventuallyIntFromString(string: String): Future[Int] = Future(string.toInt)

    /*"print every 3 seconds" in {
      val printSongs = Iteratee.foreach[Set[String]](strings => strings.map { println })
      futuresToEnumerator() |>> printSongs

      Thread.sleep(4000)

      println("exit")
    }*/

    "print every 3 seconds" in {
      val strings: Enumerator[String] = futuresToEnumerator()

      val toInt: Enumeratee[String, Future[Int]] = Enumeratee.map[String]{ s => Future { s.toInt } }

      val ints: Enumerator[Future[Int]] = strings &> toInt
      val ints2: Enumerator[Int] = Enumerator(1, 2, 3)


      val toNotFutures = Enumeratee.map[Future[Int], Int]{ f => f }


      val printInts = Iteratee.foreach[Future[Int]] { futureInt => futureInt onComplete {
          case Success(int) => int.toString
          case Failure(f) => "failure"
        }
      }
//      val printInts2 = Iteratee.foreach[Int](int => println(int))

      val notFutures = ints run printInts
//      ints2 |>> printInts2


      Thread.sleep(4000)

      println("exit")
    }
  }
}