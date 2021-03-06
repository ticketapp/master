import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import tracksDomain.Track

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestPlaylistController extends GlobalApplicationForControllers {

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
        VALUES( (SELECT playlistid FROM playlists WHERE name = 'playlist0'), '13894e56-08d1-4c1f-b3e4-466c069d15ed', 2);
        """),
      5.seconds)
  }

  "playlist controller" should {

    "create and delete a playlist" in {
      val trackId = UUID.randomUUID
      val trackId1 = UUID.randomUUID
      val trackId2 = UUID.randomUUID
      val track = Track(trackId, "title", "urlPlaylistControllerTest", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None)
      val track1 = Track(trackId1, title = "title2", url = "urlPlaylistControllerTest2", platform = 's',
        thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
        redirectUrl = None, confidence = 0.0)
      val track2 = Track(trackId2, "title3", "urlPlaylistControllerTest3", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)
      await(trackMethods.save(track))
      await(trackMethods.save(track1))
      await(trackMethods.save(track2))
      val jsonPlaylist =
        """{ "name": "PlaylistTest",
           "trackIds": [
             {
               "trackId": """" + trackId + """",
               "trackRank": 1
             },
             {
               "trackId": """" + trackId1 + """",
               "trackRank": 3
             },
             {
               "trackId": """" + trackId2 + """",
               "trackRank": 2
             }
           ]
          }"""

      val Some(result) = route(FakeRequest(playlistsDomain.routes.PlaylistController.create())
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK

      val Some(delete) = route(FakeRequest(playlistsDomain.routes.PlaylistController.delete(2))
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(delete) mustEqual OK

      await(trackMethods.delete(trackId)) mustEqual 1
    }

    "find playlists by user" in {
      val Some(playlists) = route(FakeRequest(playlistsDomain.routes.PlaylistController.findByUser())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(playlists) must contain(""""name":"playlist0"""")
    }

    "update a playlist" in {
      val jsonUpdatedPlaylist = Json.parse(
        """{
          "id": 1,
          "name": "PlaylistTest",
          "trackIds": [
            {
              "trackId": "02894e56-08d1-4c1f-b3e4-466c069d15ed",
              "trackRank": 1
            }
          ]
        }""")

      val Some(update) = route(FakeRequest(playlistsDomain.routes.PlaylistController.update(1))
        .withJsonBody(jsonUpdatedPlaylist)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(update) mustEqual OK
    }
  }
}

