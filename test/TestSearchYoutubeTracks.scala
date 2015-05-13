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
      
      whenReady (futureSeqTupleEchonestIdFacebookId, timeout(Span(2, Seconds))) { seqTupleEchonestIdFacebookId =>
        seqTupleEchonestIdFacebookId should contain allOf (("ARWRC4A1187FB5B7D5", "114140585267550"),
          ("AR3KOM61187B99740F", "112089615877"))
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

    "return " in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))
      val tracksTitle = Set("Le poinçonneur des Lilas")

      val expectedTracks = Set(Track(None,
        ", Le Poinçonneur des Lilas, 1959", "E8ZCvYg5-ZQ", 'y', "https://i.ytimg.com/vi/E8ZCvYg5-ZQ/default.jpg",
        "facebookUrl3", None, None), Track(None, """"Le poinçonneur des Lilas" (live officiel) - Archive INA""",
        "25EzXPQUWRc", 'y', "https://i.ytimg.com/vi/25EzXPQUWRc/default.jpg", "facebookUrl3", None, None),
        Track(None, "Le Poinçonneur Des Lilas", "f8PrD6FnSbw", 'y', "https://i.ytimg.com/vi/f8PrD6FnSbw/default.jpg",
          "facebookUrl3", None, None), Track(None, "Le Poinçonneur Des Lilas (1958)", "JHpUlLzt8_o", 'y',
          "https://i.ytimg.com/vi/JHpUlLzt8_o/default.jpg", "facebookUrl3", None, None), Track(None,
          "Le Poinconneur Des Lilas [SUBTITLED]", "JVSRMxZU1dY", 'y', "https://i.ytimg.com/vi/JVSRMxZU1dY/default.jpg",
          "facebookUrl3", None, None))

      whenReady(getYoutubeTracksByTitlesAndArtistName(artist, tracksTitle), timeout(Span(5, Seconds))) { tracks =>
        tracks mustBe expectedTracks
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