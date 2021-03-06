import java.util.UUID

import artistsDomain.Artist
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsValue, Json}
import testsHelper.GlobalApplicationForModels
import tracksDomain.Track


class TestSearchYoutubeTracks extends GlobalApplicationForModels {

  "SearchYoutubeTracks" must {

    "return a list of echonestId/facebookId" in {
      val futureSeqTupleEchonestIdFacebookId = searchYoutubeTrack.getSeqTupleEchonestIdFacebookId("rone")

      whenReady(futureSeqTupleEchonestIdFacebookId, timeout(Span(5, Seconds))) { seqTupleEchonestIdFacebookId =>
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

      whenReady(searchYoutubeTrack.getMaybeEchonestIdByFacebookId(artist), timeout(Span(10, Seconds))) { maybeEchonestId =>
        maybeEchonestId mustBe Option("ARNJ7441187B999AFD")
      }
    }

    "return some tracks" in {
      whenReady(searchYoutubeTrack.getEchonestSongsOnEchonest(0, "ARNJ7441187B999AFD"), timeout(Span(10, Seconds))) {
        echonestSongs: JsValue =>
        val tracks = searchYoutubeTrack.readEchonestSongs(echonestSongs)

        tracks should not be empty
      }
    }

    "return a Set[String] Enumerator of Youtube tracks title" in {
      val enumerateSongs = searchYoutubeTrack.getEchonestSongs("ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[String]](_ should not be empty)

      whenReady(enumerateSongs |>> iteratee, timeout(Span(30, Seconds))) { any => any }
    }

    "return a set of tracks for a title" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas (1958)", "JHpUlLzt8_o", 'y',
          "https://i.ytimg.com/vi/JHpUlLzt8_o/default.jpg", "facebookUrl3", "Serge Gainsbourg", None)

      whenReady(searchYoutubeTrack.getYoutubeTracksByArtistAndTitle(artist, "Le Poinçonneur Des Lilas"), timeout(Span(10, Seconds))) {
        tracks => val tracksTitle = tracks.map { track => track.title }
          tracksTitle should contain (expectedTrack.title)
      }
    }

    "return a set of tracks for a set of titles" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))
      val tracksTitle = Set("Le poinçonneur des Lilas")

      val expectedTrack = Track(UUID.randomUUID, "Le Poinçonneur Des Lilas", "f8PrD6FnSbw", 'y',
        "https://i.ytimg.com/vi/f8PrD6FnSbw/default.jpg", "facebookUrl3", "Serge Gainsbourg", None)

      whenReady(searchYoutubeTrack.getYoutubeTracksByTitlesAndArtistName(artist, tracksTitle),
        timeout(Span(10, Seconds))) { tracks =>
        val tracksTitle = tracks.map(track => track.title)
        tracksTitle should contain (expectedTrack.title)
      }
    }

    "return an enumerator of tracks" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", None, None, "facebookUrl3", Set.empty)

      val enumerateYoutubeTracks = searchYoutubeTrack.getYoutubeTracksByEchonestId(artist, "ARNJ7441187B999AFD")

      val iteratee = Iteratee.foreach[Set[Track]](_ should not be empty)

      whenReady(enumerateYoutubeTracks |>> iteratee, timeout(Span(30, Seconds))) { any => any }
    }

    "return a set of the Youtube channel Ids" in {
      val websites = Set("www.qds.com", "https://www.youtube.com/user/TheOfficialSkrillex",
        "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA")

      val expectedIds = Set("UCGWpjrgMylyGVRIKQdazrPA")

      searchYoutubeTrack.filterAndNormalizeYoutubeChannelIds(websites) mustBe expectedIds
    }

    "return the names of the Youtube user" in {
      val websites = Set("youtube.com/user/theofficialskrillex/videos")

      val expectedNames = Set("theofficialskrillex")

      searchYoutubeTrack.searchYoutubeUserNames(websites) mustBe expectedNames
    }

    "return some tracks for a Youtube User" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("hungrymusic.fr","soundcloud.com/worakls","hungrymusic.fr",
          "youtube.com/user/worakls/videos","twitter.com/worakls","facebook.com/worakls","hungrymusic.fr",
          "youtube.com/user/worakls/videos","twitter.com/worakls","facebook.com/worakls"))

      whenReady(searchYoutubeTrack.getYoutubeTracksByYoutubeUser(artist), timeout(Span(10, Seconds))) {tracks =>

        tracks should not be empty
      }
    }

    "return ids of user youtube" in {
      val artist = Artist(None, Option("139247202797113"), "Serge Gainsbourg", Option("imagePath"),
        Option("description"), "facebookUrl3", Set("website"))
      val userNames = "theofficialskrillex"
      val expectedIds = Set("UC_TVqp_SyG6j5hG-xVRy95A")
      val eventuallyYoutubeIds = searchYoutubeTrack.getYoutubeChannelIdsByUserName(artist, userNames)

      whenReady(eventuallyYoutubeIds, timeout(Span(10, Seconds))) { _ mustBe expectedIds }
    }

    "return a set of youtube tracks" in {
      val artist = Artist(id = None,
        facebookId = None,
        name = "Skrillex",
        imagePath = None,
        description = None,
        facebookUrl = "facebookUrl3",
        websites = Set.empty)

      val youtubeChannel = "UCGWpjrgMylyGVRIKQdazrPA"
      val youtubeChannel2 = "UCswumVYAf0SKczPSoRGHxcQ"

      val uuid = UUID.randomUUID

      val expectedTrack = Track(uuid, "Welcome to Topsify", "ISo15c2zKa4", 'y',
        "https://i.ytimg.com/vi/ISo15c2zKa4/default.jpg", "facebookUrl3", "Skrillex", None)

      val eventuallyYoutubeTracks = searchYoutubeTrack.getYoutubeTracksByChannelId(artist, youtubeChannel)

      whenReady(eventuallyYoutubeTracks, timeout(Span(10, Seconds))) { tracks =>

        tracks.map { _.copy(uuid = uuid) } should contain (expectedTrack)

        whenReady(eventuallyYoutubeTracks, timeout(Span(10, Seconds))) { tracksFromYoutubeChannel2 =>

          tracksFromYoutubeChannel2 should not be empty
        }
      }
    }

    "return set of youtube tracks by websites with channel id" in {
      val artist = Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
        Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
          "youtube.com/user/feuchatterton", "youtube.com/user/feuchatterton",
          "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"))
      whenReady(searchYoutubeTrack.getYoutubeTracksByChannel(artist), timeout(Span(10, Seconds))) { tracks =>
        assert(tracks.isInstanceOf[Set[Track]])
        tracks should not be empty
      }
    }
  }
}
