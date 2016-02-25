import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import database.MyPostgresDriver.api._
import genresDomain.Genre
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import playlistsDomain._
import testsHelper.GlobalApplicationForModelsIntegration
import tracksDomain.{Track, TrackWithGenres}
import userDomain.User

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class PlaylistModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO playlists(userId, name) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', 'playlist0');

        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO artists(artistid, facebookid, name, facebookurl)
          VALUES('300', 'facebookIdTestTrack', 'artistTest', 'artistFacebookUrlTestPlaylistModel');

        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');

        INSERT INTO playliststracks(playlistId, trackid, trackrank)
          VALUES((SELECT playlistid FROM playlists WHERE name = 'playlist0'), '02894e56-08d1-4c1f-b3e4-466c069d15ed', 1);
        INSERT INTO playliststracks(playlistId, trackid, trackrank)
          VALUES((SELECT playlistid FROM playlists WHERE name = 'playlist0'), '13894e56-08d1-4c1f-b3e4-466c069d15ed', 2);

        INSERT INTO genres(name, icon) VALUES('genretest0', 'a');
        INSERT INTO genres(name, icon) VALUES('genretest00', 'a');

        INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
         ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
          (SELECT genreid FROM genres WHERE name = 'genretest0'), 1);
        INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
         ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
          (SELECT genreid FROM genres WHERE name = 'genretest00'), 1);
        """),
      5.seconds)
  }

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
      whenReady(playlistMethods.findByUserId(defaultUserUUID), timeout(Span(5, Seconds))) { foundPlaylists =>
        val playlistId = foundPlaylists.head.playlistInfo.playlistId

        val expectedTrack1 = TrackWithGenres(
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
          genres = Vector(Genre(Some(1), "genretest0", 'a'), Genre(Some(2), "genretest00", 'a')))

        val expectedTrack2 =
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
            genres = Vector(Genre(Some(1), "genretest0", 'a'), Genre(Some(2), "genretest00", 'a')))

        val expectedPlaylistTracks = Vector(
          TrackWithPlaylistRankAndGenres(expectedTrack1, 1.0),
          TrackWithPlaylistRankAndGenres(expectedTrack2, 2.0))

        val expectedPlaylist = PlaylistWithTracksWithGenres(
          playlistInfo = Playlist(playlistId = playlistId, userId = defaultUserUUID, name = "playlist0"),
          tracksWithRankAndGenres = expectedPlaylistTracks)

        foundPlaylists must contain(expectedPlaylist)
      }
    }

    "be updated" in {
      val userUUID = defaultUserUUID
      val playlistToUpdate = PlaylistWithTracksIdAndRank(
        playlistInfo = Playlist(
          playlistId = Some(1),
          userId = userUUID,
          name = "playlistUpdated"),
        tracksWithRank = Vector(TrackIdWithPlaylistRank(
          trackId = UUID.fromString("02894e56-08d1-4c1f-b3e4-466c069d15ed"),
          rank = 1.0)))

      whenReady(playlistMethods.update(playlist = playlistToUpdate), timeout(Span(5, Seconds))) { result =>
        whenReady(playlistMethods.findByUserId(userUUID), timeout(Span(5, Seconds))) { foundPlaylists =>
          val expectedUpdatedPlaylist = PlaylistWithTracksWithGenres(
            playlistInfo = playlistToUpdate.playlistInfo.copy(playlistId = Option(result)),
            tracksWithRankAndGenres = Vector(
              TrackWithPlaylistRankAndGenres(
                track = TrackWithGenres(
                  track = Track(UUID.fromString("02894e56-08d1-4c1f-b3e4-466c069d15ed"), "title", "url0", 'y',
                    "thumbnailUrl", "facebookUrl0", "artistName", None, 0.0),
                  genres = Vector(Genre(Some(1), "genretest0", 'a'), Genre(Some(2), "genretest00", 'a'))), 1.0)))

          assert(result > 0)
          foundPlaylists should contain only expectedUpdatedPlaylist
        }
      }
    }
  }
}
