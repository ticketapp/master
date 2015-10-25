import java.util.UUID

import models._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestPlaylistModel extends PlaySpec with BeforeAndAfterAll with OneAppPerSuite with Injectors {

  var artistId = -1L
  val artist = Artist(None, Option("facebookIdTestTrack"), "artistTest", Option("imagePath"),
    Option("description"), "artistFacebookUrlTestPlaylistModel", Set("website"))

  override def beforeAll() = {
    try {
      artistId = Await.result(artistMethods.save(artist), 2 seconds).id.get
    } catch {
      case e: Exception => Logger.info("TestPlaylistModel.beforeAll:" + e.getMessage)
    }
  }

  override def afterAll() = {
    Await.result(artistMethods.delete(artistId), 2 seconds)
  }

  "A playlist" must {

    "be able to be saved and deleted" in {
      val playlist = Playlist(None, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), "name")
      whenReady(playlistMethods.save(playlist), timeout(Span(5, Seconds))) { savedPlaylist =>
        val maybePlaylistId = savedPlaylist.playlistId

        savedPlaylist mustBe playlist.copy(playlistId = maybePlaylistId)

        whenReady(playlistMethods.find(maybePlaylistId.get), timeout(Span(5, Seconds))) { foundPlaylist =>

          foundPlaylist mustBe Option(PlaylistWithTracks(savedPlaylist, Vector.empty))

          whenReady(playlistMethods.delete(maybePlaylistId.get), timeout(Span(5, Seconds))) {

            _ mustBe 1
          }
        }
      }
    }

    "be able to be saved with its tracks, rendered sorted by rank and deleted" in {
      val trackId1 = UUID.randomUUID
      val track1 = Track(trackId1, "title", "urlPlaylistTest", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None)
      val trackId2 = UUID.randomUUID
      val track2 = Track(uuid = trackId2, title = "title2", url = "urlPlaylistTest2", platform = 's',
        thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
        redirectUrl = None, confidence = 0.0)
      val trackId3 = UUID.randomUUID
      val track3 = Track(trackId3, "title3", "urlPlaylistTest3", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)
      whenReady(trackMethods.save(track1), timeout(Span(5, Seconds))) { _ =>
        whenReady(trackMethods.save(track2), timeout(Span(5, Seconds))) { _ =>
          whenReady(trackMethods.save(track3), timeout(Span(5, Seconds))) { _ =>
            val userUUID = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae")
            val playlist = Playlist(None, userUUID, "name2")
            whenReady(playlistMethods.save(playlist), timeout(Span(5, Seconds))) { savedPlaylist =>
              val playlistId = savedPlaylist.playlistId
              try {
                whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId.get, trackId1, 2)),
                  timeout(Span(5, Seconds))) { _ =>
                  whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId.get, trackId2, 3)),
                    timeout(Span(5, Seconds))) { _ =>
                    whenReady(playlistMethods.saveTrackRelation(PlaylistTrack(playlistId.get, trackId3, 1)),
                      timeout(Span(5, Seconds))) { _ =>

                      whenReady(trackMethods.findByPlaylistId(playlistId.get), timeout(Span(5, Seconds))) { tracks =>
                        tracks should contain allOf(
                          TrackWithPlaylistRank(track1, 2),
                          TrackWithPlaylistRank(track2, 3),
                          TrackWithPlaylistRank(track3, 1))
                      }

                      whenReady(playlistMethods.find(playlistId.get), timeout(Span(5, Seconds))) { playlistWithTracks =>
                        playlistWithTracks mustBe Option(PlaylistWithTracks(
                          Playlist(playlistId, userUUID, "name2"), Vector(
                            TrackWithPlaylistRank(Track(
                              uuid = trackId1,
                              title = "title",
                              url = "urlPlaylistTest",
                              platform = 's',
                              thumbnailUrl = "thumbnailUrl",
                              artistFacebookUrl = "artistFacebookUrlTestPlaylistModel",
                              artistName = "name",
                              redirectUrl = None,
                              confidence = 0.0),
                              rank = 2.0),
                            TrackWithPlaylistRank(Track(
                              uuid = trackId2,
                              title = "title2",
                              url = "urlPlaylistTest2",
                              platform = 's',
                              thumbnailUrl = "thumbnailUrl",
                              artistFacebookUrl = "artistFacebookUrlTestPlaylistModel",
                              artistName = "name",
                              redirectUrl = None,
                              confidence = 0.0),
                              rank = 3.0),
                            TrackWithPlaylistRank(Track(
                              uuid = trackId3,
                              title = "title3",
                              url = "urlPlaylistTest3",
                              platform = 's',
                              thumbnailUrl = "thumbnailUrl",
                              artistFacebookUrl = "artistFacebookUrlTestPlaylistModel",
                              artistName = "name",
                              redirectUrl = None,
                              confidence = 2.0),
                              rank = 1.0))))
                      }

                      whenReady(playlistMethods.delete(playlistId.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
                    }
                  }
                }
              } finally {
                trackMethods.delete(trackId1)
                trackMethods.delete(trackId2)
                trackMethods.delete(trackId3)
              }
            }
          }
        }
      }
    }
  }
}
