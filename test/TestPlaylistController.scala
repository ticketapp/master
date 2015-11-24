import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import tracksDomain.Track


class TestPlaylistController extends GlobalApplicationForControllers {
  sequential

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

