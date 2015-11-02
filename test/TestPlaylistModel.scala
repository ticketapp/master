import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.db.evolutions.Evolutions

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestPlaylistModel extends GlobalApplicationForModels {

  "A playlist" must {

    "be able to be saved and deleted" in {
      val loginInfo: LoginInfo = LoginInfo("providerId231", "providerKey231")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName123"),
        lastName = Option("lastName123"),
        fullName = Option("fullName123"),
        email = Option("email12345"),
        avatarURL = Option("avatarUrl"))
      whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
        val playlist = Playlist(None, uuid, "name")
        val trackId = UUID.randomUUID
        val trackId1 = UUID.randomUUID
        val trackId2 = UUID.randomUUID
        val track = Track(trackId, "title", "urlPlaylistTest", 's', "thumbnailUrl",
            "artistFacebookUrlTestPlaylistModel", "name", None)
        val track1 = Track(trackId1, title = "title2", url = "urlPlaylistTest2", platform = 's',
            thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
            redirectUrl = None, confidence = 0.0)
        val track2 = Track(trackId2, "title3", "urlPlaylistTest3", 's', "thumbnailUrl",
            "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)

        val trackIdWithPlaylistRankSeq = Seq(
          TrackIdWithPlaylistRank(trackId, 1.0),
          TrackIdWithPlaylistRank(trackId1, 1.5),
          TrackIdWithPlaylistRank(trackId2, 1.2)
        )
        val tracksWithPlaylistRank = Vector (
          TrackWithPlaylistRank(track, 1.0),
          TrackWithPlaylistRank(track2, 1.2),
          TrackWithPlaylistRank(track1, 1.5)
        )
        Await.result(trackMethods.save(track), 2 seconds)
        Await.result(trackMethods.save(track1), 2 seconds)
        Await.result(trackMethods.save(track2), 2 seconds)

        whenReady(playlistMethods.saveWithTrackRelations(PlaylistWithTracksIdAndRank(playlist, trackIdWithPlaylistRankSeq.toVector)),
          timeout(Span(5, Seconds))) { savedPlaylist =>

          val maybePlaylistId = savedPlaylist

          try {
            whenReady(playlistMethods.find(savedPlaylist), timeout(Span(5, Seconds))) { foundPlaylist =>

              foundPlaylist mustBe Option(PlaylistWithTracks(playlist.copy(playlistId = Option(savedPlaylist)),
                tracksWithPlaylistRank))

            }
          } finally {
            whenReady(playlistMethods.delete(maybePlaylistId), timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1

                Await.result(trackMethods.delete(trackId), 2 seconds)
                Await.result(trackMethods.delete(trackId1), 2 seconds)
                Await.result(trackMethods.delete(trackId2), 2 seconds)
              }
            }
          }
        }
      }
    }

    "find playlists by userUUID" in {
      val loginInfo: LoginInfo = LoginInfo("providerId23189", "providerKey23187")
      val uuid: UUID = UUID.randomUUID()
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName12345"),
        lastName = Option("lastName123"),
        fullName = Option("fullName123"),
        email = Option("email1234587"),
        avatarURL = Option("avatarUrl"))
      whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
        val playlist = Playlist(None, uuid, "name")
        val trackId = UUID.randomUUID
        val trackId1 = UUID.randomUUID
        val trackId2 = UUID.randomUUID
        val track = Track(trackId, "title", "urlPlaylistTest", 's', "thumbnailUrl",
          "artistFacebookUrlTestPlaylistModel", "name", None)
        val track1 = Track(trackId1, title = "title2", url = "urlPlaylistTest2", platform = 's',
          thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
          redirectUrl = None, confidence = 0.0)
        val track2 = Track(trackId2, "title3", "urlPlaylistTest3", 's', "thumbnailUrl",
          "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)

        val trackIdWithPlaylistRankSeq = Seq(
          TrackIdWithPlaylistRank(trackId, 1.0),
          TrackIdWithPlaylistRank(trackId1, 1.5),
          TrackIdWithPlaylistRank(trackId2, 1.2)
        )
        val tracksWithPlaylistRank = Vector (
          TrackWithPlaylistRank(track, 1.0),
          TrackWithPlaylistRank(track2, 1.2),
          TrackWithPlaylistRank(track1, 1.5)
        )
        Await.result(trackMethods.save(track), 2 seconds)
        Await.result(trackMethods.save(track1), 2 seconds)
        Await.result(trackMethods.save(track2), 2 seconds)

        whenReady(playlistMethods.saveWithTrackRelations(PlaylistWithTracksIdAndRank(playlist, trackIdWithPlaylistRankSeq.toVector)),
          timeout(Span(5, Seconds))) { savedPlaylist =>

          val maybePlaylistId = savedPlaylist

          try {
            whenReady(playlistMethods.findByUserId(uuid), timeout(Span(5, Seconds))) { foundPlaylist =>

              foundPlaylist must contain(PlaylistWithTracks(playlist.copy(playlistId = Option(savedPlaylist)),
                tracksWithPlaylistRank))
            }
          } finally {
            whenReady(playlistMethods.delete(maybePlaylistId), timeout(Span(5, Seconds))) { response =>

              response mustBe 1

              whenReady(userDAOImpl.delete(uuid), timeout(Span(5, Seconds))) { response1 =>

                response1 mustBe 1
              }
            }
          }
        }
      }
    }
  }
}
