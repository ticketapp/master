import java.util.{UUID, Date}
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
      whenReady (getEchonestSongsOnEchonest(0, "ARNJ7441187B999AFD"), timeout(Span(2, Seconds))) { echonestSongs: JsValue =>
        val songs = readEchonestSongs(echonestSongs)

        songs should not be empty
      }
    }

    "return an Enumerator of Set[String]" in {
      val enumerateSongs = getEchonestSongs("ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[String]](_ should not be empty)

      whenReady(enumerateSongs |>> iteratee, timeout(Span(5, Seconds))) { any => any }
    }

    "return a set of tracks for a title" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas (1958)", "JHpUlLzt8_o", 'y',
          "https://i.ytimg.com/vi/JHpUlLzt8_o/default.jpg", "facebookUrl3", "Serge Gainsbourg", None, None)

      whenReady(getYoutubeTracksByArtistAndTitle(artist, "Le Poinçonneur Des Lilas"), timeout(Span(5, Seconds))) {
        tracks => val tracksTitle = tracks.map { track => track.title}
          tracksTitle should contain (expectedTrack.title)
      }
    }

    "return a set of tracks for a set of titles" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))
      val tracksTitle = Set("Le poinçonneur des Lilas")

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas", "f8PrD6FnSbw", 'y',
        "https://i.ytimg.com/vi/f8PrD6FnSbw/default.jpg", "facebookUrl3", "Serge Gainsbourg", None, None)

      whenReady(getYoutubeTracksByTitlesAndArtistName(artist, tracksTitle), timeout(Span(5, Seconds))) { tracks =>
        val tracksTitle = tracks.map { track => track.title}
        tracksTitle should contain (expectedTrack.title)
      }
    }

    "return an enumerator of tracks" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val enumerateYoutubeTracks = getYoutubeTracksByEchonestId(artist, "ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[Track]](a => {
        a should not be empty
      })

      whenReady(enumerateYoutubeTracks |>> iteratee, timeout(Span(10, Seconds))) { any => any }
    }

    "return a set of the Youtube channel Ids" in {
      val websites = Set("www.qds.com", "https://www.youtube.com/user/TheOfficialSkrillex",
        "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA")


      val expectedIds = Set("UCGWpjrgMylyGVRIKQdazrPA")

      getYoutubeChannelId(websites) mustBe expectedIds
    }

    "return names of user youtube" in {
      val websites = Set("youtube.com/theofficialskrillex")

      val expectedNames = Set("theofficialskrillex")

      getYoutubeUserNames(websites) mustBe expectedNames
    }

    "return ids of user youtube" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val userNames = "theofficialskrillex"

      val expectedIds = Set("UC_TVqp_SyG6j5hG-xVRy95A")
      val eventuallyYoutubeId = getYoutubeChannelIdsByUserName(artist, userNames)
      whenReady(eventuallyYoutubeId, timeout(Span(10, Seconds))) { _ mustBe expectedIds }
    }

    "return set of youtube tracks" in {
      val youtubeChannel = "UCGWpjrgMylyGVRIKQdazrPA"

      val artist = Artist(None, Option("139247202797113"), "Skrillex", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val uuid = UUID.fromString("04d64aef-2baa-42b3-a0dc-07f77da9303d")

      val expectedTrack = Track(uuid, "Welcome to Topsify", "ISo15c2zKa4", 'y',
        "https://i.ytimg.com/vi/ISo15c2zKa4/default.jpg", "facebookUrl3", "Skrillex", None, None, None, List())

      val eventuallyYoutubeTracks = getYoutubeTracksByChannelId(artist, youtubeChannel)

      whenReady(eventuallyYoutubeTracks, timeout(Span(10, Seconds))) { tracks =>
        tracks.map { _.copy(trackId = uuid) } should contain (expectedTrack)
      }
    }
  }
}