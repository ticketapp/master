import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}

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
          timeout(Span(5, Seconds))) { savedPlaylistId =>

          whenReady(playlistMethods.find(savedPlaylistId), timeout(Span(5, Seconds))) { foundPlaylist =>

            foundPlaylist mustBe Option(PlaylistWithTracks(playlist.copy(playlistId = Option(savedPlaylistId)),
              tracksWithPlaylistRank))
          }

          whenReady(playlistMethods.delete(savedPlaylistId), timeout(Span(5, Seconds))) { response =>

            response mustBe 1
          }
        }
      }
    }

    "find playlists by userUUID" in {
      val userUUID = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      whenReady(playlistMethods.findByUserId(userUUID), timeout(Span(5, Seconds))) { foundPlaylist =>
        val playlistId = foundPlaylist.head.playlistInfo.playlistId

        val track1 = TrackWithGenres(
          track = Track(
            uuid = UUID.fromString("02894e56-08d1-4c1f-b3e4-466c069d15ed"),
            title = "title",
            url = "url0",
            platform = 'y',
            thumbnailUrl =  "thumbnailUrl",
            artistFacebookUrl = "facebookUrl0",
            artistName = "artistName",
            redirectUrl = None,
            confidence = 0.0),
          genres = Vector(Genre(Some(1), "genreTest0", 'a'), Genre(Some(2), "genreTest00", 'a')))

        val track2 =
          TrackWithGenres(
            track = Track(
              uuid = UUID.fromString("13894e56-08d1-4c1f-b3e4-466c069d15ed"),
              title = "title0",
              url = "url00",
              platform = 'y',
              thumbnailUrl =  "thumbnailUrl",
              artistFacebookUrl = "facebookUrl0",
              artistName = "artistName",
              redirectUrl = None,
              confidence = 0.0),
            genres = Vector(Genre(Some(1), "genreTest0", 'a'), Genre(Some(2), "genreTest00", 'a')))

        val expectedPlaylistTracks = Vector(
          TrackWithPlaylistRankAndGenres(track1, 1.0),
          TrackWithPlaylistRankAndGenres(track2,2.0))

        val expectedPlaylist = PlaylistWithTracksWithGenres(
          playlistInfo = Playlist(playlistId = playlistId, userId = userUUID, name = "playlist0"),
          tracksWithRankAndGenres = expectedPlaylistTracks)

        foundPlaylist must contain(expectedPlaylist)
      }
    }
  }
}
