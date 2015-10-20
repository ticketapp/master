import java.util.UUID

import models._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsValue, Json}
import services.{SearchYoutubeTracks, Utilities}

class TestSearchYoutubeTracks extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)

  "SearchYoutubeTracks" must {

    "return a list of echonestId/facebookId" in {
      val futureSeqTupleEchonestIdFacebookId = searchYoutubeTrack.getSeqTupleEchonestIdFacebookId("rone")
      
      whenReady (futureSeqTupleEchonestIdFacebookId, timeout(Span(2, Seconds))) { seqTupleEchonestIdFacebookId =>
        seqTupleEchonestIdFacebookId should contain allOf (("ARWRC4A1187FB5B7D5", "114140585267550"),
          ("AR3KOM61187B99740F", "112089615877"))
      }
    }

    "return an artist name" in {
      val echonestWSResponse: JsValue = Json.parse(
        """{"response":{"status":{"version":"4.2","code":0,"message":"Success"},
          |"artist":{"id":"ARNJ7441187B999AFD","name":"Serge Gainsbourg"}}}""".stripMargin)

      searchYoutubeTrack.getEchonestIdIfSameName(echonestWSResponse, "serge gainsbourg") mustBe Option("ARNJ7441187B999AFD")
    }

    "return echonestId" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      whenReady (searchYoutubeTrack.getMaybeEchonestIdByFacebookId(artist), timeout(Span(2, Seconds))) { maybeEchonestId =>
        maybeEchonestId mustBe Option("ARNJ7441187B999AFD")
      }
    }

    "return songs" in {
      whenReady (searchYoutubeTrack.getEchonestSongsOnEchonest(0, "ARNJ7441187B999AFD"), timeout(Span(2, Seconds))) {
        echonestSongs: JsValue =>
        val songs = searchYoutubeTrack.readEchonestSongs(echonestSongs)

        songs should not be empty
      }
    }

    "return an Enumerator of Set[String]" in {
      val enumerateSongs = searchYoutubeTrack.getEchonestSongs("ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[String]](_ should not be empty)

      whenReady(enumerateSongs |>> iteratee, timeout(Span(5, Seconds))) { any => any }
    }

    "return a set of tracks for a title" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas (1958)", "JHpUlLzt8_o", 'y',
          "https://i.ytimg.com/vi/JHpUlLzt8_o/default.jpg", "facebookUrl3", "Serge Gainsbourg", None, None)

      whenReady(searchYoutubeTrack.getYoutubeTracksByArtistAndTitle(artist, "Le Poinçonneur Des Lilas"), timeout(Span(5, Seconds))) {
        tracks => val tracksTitle = tracks.map { track => track.title }
          tracksTitle should contain (expectedTrack.title)
      }
    }

    "return a set of tracks for a set of titles" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))
      val tracksTitle = Set("Le poinçonneur des Lilas")

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas", "f8PrD6FnSbw", 'y',
        "https://i.ytimg.com/vi/f8PrD6FnSbw/default.jpg", "facebookUrl3", "Serge Gainsbourg", None, None)

      whenReady(searchYoutubeTrack.getYoutubeTracksByTitlesAndArtistName(artist, tracksTitle), timeout(Span(5, Seconds))) { tracks =>
        val tracksTitle = tracks.map { track => track.title}
        tracksTitle should contain (expectedTrack.title)
      }
    }

    "return an enumerator of tracks" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val enumerateYoutubeTracks = searchYoutubeTrack.getYoutubeTracksByEchonestId(artist, "ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[Track]](_ should not be empty)

      whenReady(enumerateYoutubeTracks |>> iteratee, timeout(Span(10, Seconds))) { any => any }
    }

    "return a set of the Youtube channel Ids" in {
      val websites = Set("www.qds.com", "https://www.youtube.com/user/TheOfficialSkrillex",
        "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA")


      val expectedIds = Set("UCGWpjrgMylyGVRIKQdazrPA")

      searchYoutubeTrack.filterAndNormalizeYoutubeChannelIds(websites) mustBe expectedIds
    }

    "return names of user youtube" in {
      val websites = Set("youtube.com/user/theofficialskrillex/videos")

      val expectedNames = Set("theofficialskrillex")

      searchYoutubeTrack.getYoutubeUserNames(websites) mustBe expectedNames
    }

    "return tracks for YT User" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("hungrymusic.fr","soundcloud.com/worakls","hungrymusic.fr",
          "youtube.com/user/worakls/videos","twitter.com/worakls","facebook.com/worakls","hungrymusic.fr",
          "youtube.com/user/worakls/videos","twitter.com/worakls","facebook.com/worakls"))
      whenReady(searchYoutubeTrack.getYoutubeTracksByYoutubeUser(artist), timeout(Span(5, Seconds))) {tracks =>
        assert(tracks.isInstanceOf[Set[Track]])
        tracks should not be empty
      }
    }

    "return ids of user youtube" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val userNames = "theofficialskrillex"

      val expectedIds = Set("UC_TVqp_SyG6j5hG-xVRy95A")
      val eventuallyYoutubeId = searchYoutubeTrack.getYoutubeChannelIdsByUserName(artist, userNames)
      whenReady(eventuallyYoutubeId, timeout(Span(10, Seconds))) { _ mustBe expectedIds }
    }

    "return set of youtube tracks" in {
      val youtubeChannel = "UCGWpjrgMylyGVRIKQdazrPA"

      val artist = Artist(None, Option("139247202797113"), "Skrillex", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val uuid = UUID.fromString("04d64aef-2baa-42b3-a0dc-07f77da9303d")

      val expectedTrack = Track(uuid, "Welcome to Topsify", "ISo15c2zKa4", 'y',
        "https://i.ytimg.com/vi/ISo15c2zKa4/default.jpg", "facebookUrl3", "Skrillex", None, None/*, None, List()*/)

      val eventuallyYoutubeTracks = searchYoutubeTrack.getYoutubeTracksByChannelId(artist, youtubeChannel)

      whenReady(eventuallyYoutubeTracks, timeout(Span(10, Seconds))) { tracks =>
        tracks.map { _.copy(uuid = uuid) } should contain (expectedTrack)
      }
    }

    "return set of youtube tracks by websites" in {
      val artist = Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
        Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
          "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA")/*,List(),List()*/,None,None)
      whenReady(searchYoutubeTrack.getYoutubeTracksByYoutubeUser(artist), timeout(Span(5, Seconds))) {tracks =>
        assert(tracks.isInstanceOf[Set[Track]])
      }
    }

    "return set of youtube tracks by websites with channel id" in {
      val artist = Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
        Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
          "youtube.com/user/feuchatterton", "youtube.com/user/feuchatterton",
          "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA")/*,List(),List()*/,None,None)
      whenReady(searchYoutubeTrack.getYoutubeTracksByChannel(artist), timeout(Span(5, Seconds))) { tracks =>
        assert(tracks.isInstanceOf[Set[Track]])
        tracks should not be empty
      }
    }
  }
}
