import java.util.Date
import controllers.DAOException
import models.{Track, Artist}
import models.Artist._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.concurrent.{Promise, Future}
import scala.util.Success
import scala.util.Failure
import services.SearchYoutubeTracks._
import play.api.libs.concurrent.Execution.Implicits._

class TestSearchYoutubeTracks extends PlaySpec with OneAppPerSuite {

  "SearchYoutubeTracks" must {

    "return a list of echonestId/facebookId" in {
      val futureSeqTupleEchonestIdFacebookId = getSeqTupleEchonestIdFacebookId("rone")
      
      val expectedResponse = List(("ARWRC4A1187FB5B7D5", "114140585267550"), ("AR4CUEZ1187B9B75A6", "39109015282"),
        ("AR3KOM61187B99740F", "112089615877"), ("ARTCFOI11F4C83CCCD", "108719029161362"),
        ("ARVVHOF1269FB35B7D", "166601410076014"), ("ARUQRKV12AF7D959A6", "131569216902639"))

      futureSeqTupleEchonestIdFacebookId shouldBe a [Future[Seq[(String, String)]]]

      whenReady (futureSeqTupleEchonestIdFacebookId, timeout(Span(2, Seconds))) { seqTupleEchonestIdFacebookId =>
        seqTupleEchonestIdFacebookId mustBe expectedResponse
      }
    }

    "return an artist name" in {
      val echonestResponse: JsValue = Json.parse(
        """{"response":{"status":{"version":"4.2","code":0,"message":"Success"},
          |"artist":{"id":"ARNJ7441187B999AFD","name":"Serge Gainsbourg"}}}""".stripMargin)

      getEchonestIdIfSameName(echonestResponse, "serge gainsbourg") mustBe Option("ARNJ7441187B999AFD")
    }

    "return echonestId" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      whenReady (getMaybeEchonestIdByFacebookId(artist), timeout(Span(2, Seconds))) { maybeEchonestId =>
        maybeEchonestId mustBe Option("ARNJ7441187B999AFD")
      }
    }

    "return songs" in {
      whenReady (getEchonestSongsWSCall(0, "ARNJ7441187B999AFD"), timeout(Span(2, Seconds))) { echonestSongs: JsValue =>
        val songs = readEchonestSongs(echonestSongs)

        songs should not be empty
      }
    }

    "return an Enumerator of Set[String]" in {
      val enumerateSongs = getEchonestSongs("ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[String]](_ should not be empty)

      whenReady(enumerateSongs |>> iteratee, timeout(Span(2, Seconds))) { any => any }
    }

    "test" in {
      def testt(): Future[Iteratee[String, Int]] = {
        val strings: Enumerator[String] = Enumerator("1","2","3","4")

        val sum: Iteratee[Int,Int] = Iteratee.fold[Int,Int](0){ (s,e) => s + e }

        val toInt: Enumeratee[String,Int] = Enumeratee.map[String]{ s => s.toInt }

        strings |>> toInt &>> sum
      }

      val ii = testt() onSuccess {
        case s => println(s)
      }

    }

    "return an enumerator of tracks" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val enumerateYoutubeTracks = getYoutubeTracksByEchonestId(artist, "ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[Track]](_ should not be empty)

      whenReady(enumerateYoutubeTracks |>> iteratee, timeout(Span(5, Seconds))) { any => any }
    }
  }
}