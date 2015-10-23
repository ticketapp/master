import java.util.UUID

import models.{Track, _}
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestPlaylistModel extends PlaySpec with BeforeAndAfterAll with OneAppPerSuite {
  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val playlistMethods = new PlaylistMethods(dbConfProvider, utilities)


  var artistId = -1L
  val artist = Artist(None, Option("facebookIdTestTrack"), "artistTest", Option("imagePath"),
    Option("description"), "artistFacebookUrlTestPlaylistModel", Set("website"))

  override def beforeAll() = {
    artistId = Await.result(artistMethods.save(artist), 2 seconds).id.get
  }

  override def afterAll() = {
    artistMethods.delete(artistId)
  }

  "A playlist" must {

    "be able to be saved and deleted" in {
      val playlist = Playlist(None, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), "name")
      whenReady(playlistMethods.save(playlist), timeout(Span(5, Seconds))) { savedPlaylist =>
        val maybePlaylistId = savedPlaylist.playlistId

        savedPlaylist mustBe playlist.copy(playlistId = maybePlaylistId)

        whenReady(playlistMethods.find(maybePlaylistId.get), timeout(Span(5, Seconds))) { foundPlaylistWithTracks =>

          foundPlaylistWithTracks mustBe Option(PlaylistWithTracks(savedPlaylist, Seq.empty))

          whenReady(playlistMethods.delete(maybePlaylistId.get), timeout(Span(5, Seconds))) {

            _ mustBe 1
          }
        }
      }
    }
//
//    "be able to be saved with its tracks, rendered sorted by rank and deleted" in {
//      val trackId1 = UUID.randomUUID
//      val track1 = Track(trackId1, "title", "urlPlaylistTest", 's', "thumbnailUrl",
//        "artistFacebookUrlTestPlaylistModel", "name", None)
//      val trackId2 = UUID.randomUUID
//      val track2 = Track(uuid = trackId2, title = "title2", url = "urlPlaylistTest2", platform = 's',
//        thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
//        redirectUrl = None, confidence = 0.0)
//      val trackId3 = UUID.randomUUID
//      val track3 = Track(trackId3, "title3", "urlPlaylistTest3", 's', "thumbnailUrl",
//        "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)
//      whenReady(trackMethods.save(track1), timeout(Span(5, Seconds))) { _ =>
//        whenReady(trackMethods.save(track2), timeout(Span(5, Seconds))) { _ =>
//          whenReady(trackMethods.save(track3), timeout(Span(5, Seconds))) { _ =>
//            val playlist = Playlist(None, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), "name2")
//            whenReady(playlistMethods.save(playlist), timeout(Span(5, Seconds))) { savedPlaylist =>
//              val playlistId = savedPlaylist.playlistId.get
//              try {
//                whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId, trackId1, 2)),
//                  timeout(Span(5, Seconds))) { _ =>
//                  whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId, trackId2, 3)),
//                    timeout(Span(5, Seconds))) { _ =>
//                    whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId, trackId3, 1)),
//                      timeout(Span(5, Seconds))) { _ =>
//
//                      whenReady(trackMethods.findByPlaylistId(playlistId), timeout(Span(5, Seconds))) { tracks =>
//                        tracks should contain allOf(
//                          TrackWithPlaylistRank(track1, 2),
//                          TrackWithPlaylistRank(track2, 3),
//                          TrackWithPlaylistRank(track3, 1))
//                      }
//
//                      whenReady(playlistMethods.delete(playlistId), timeout(Span(5, Seconds))) { _ mustBe 1 }
//                    }
//                  }
//                }
//              } finally {
//                trackMethods.delete(trackId1)
//                trackMethods.delete(trackId2)
//                trackMethods.delete(trackId3)
//              }
//            }
//          }
//        }
//      }
//    }
  }
}
