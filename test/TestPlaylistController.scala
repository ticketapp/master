import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.test.{FakeRequest, PlaySpecification}


class TestPlaylistController extends GlobalApplicationForControllers {
  sequential

  "playlist controller" should {

    "create and delete an playlist" in {
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

      val Some(result) = route(FakeRequest(POST, "/playlists")
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK

      val playlistSaved = await(playlistMethods.findByUserId(identity.uuid)).head

      val Some(delete) = route(FakeRequest(DELETE, "/playlists/" + playlistSaved.playlistInfo.playlistId.get)
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(delete) mustEqual OK

      await(trackMethods.delete(trackId)) mustEqual 1
      await(trackMethods.delete(trackId1)) mustEqual 1
      await(trackMethods.delete(trackId2)) mustEqual 1
    }

    "find playlists by user" in {
      val trackId = UUID.randomUUID
      val trackId1 = UUID.randomUUID
      val trackId2 = UUID.randomUUID
      val track = Track(trackId, "title", "urlPlaylistControllerTestFindByUser", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None)
      val track1 = Track(trackId1, title = "title2", url = "urlPlaylistControllerTestFindByUser2", platform = 's',
        thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
        redirectUrl = None, confidence = 0.0)
      val track2 = Track(trackId2, "title3", "urlPlaylistControllerTestFindByUser3", 's', "thumbnailUrl",
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

      val Some(result) = route(FakeRequest(POST, "/playlists")
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK

      val Some(playlists) = route(FakeRequest(GET, "/playlists")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(playlists).toString() must contain(""""title":"title","url":"urlPlaylistControllerTestFindByUser"""")
    }

    "update a playlist" in {
      val trackId = UUID.randomUUID
      val trackId1 = UUID.randomUUID
      val trackId2 = UUID.randomUUID
      val trackId3 = UUID.randomUUID
      val track = Track(trackId, "title", "urlPlaylistControllerTestFindByUser", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None)
      val track1 = Track(trackId1, title = "title2", url = "urlPlaylistControllerTestFindByUser2", platform = 's',
        thumbnailUrl = "thumbnailUrl", artistFacebookUrl = "artistFacebookUrlTestPlaylistModel", artistName = "name",
        redirectUrl = None, confidence = 0.0)
      val track2 = Track(trackId2, "title3", "urlPlaylistControllerTestFindByUser3", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)
      val track3 = Track(trackId3, "title4", "urlPlaylistControllerTestFindByUser4", 's', "thumbnailUrl",
        "artistFacebookUrlTestPlaylistModel", "name", None, 2.0)

      await(trackMethods.save(track))
      await(trackMethods.save(track1))
      await(trackMethods.save(track2))
      await(trackMethods.save(track3))

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

      val Some(result) = route(FakeRequest(POST, "/playlists")
        .withJsonBody(Json.parse(jsonPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK


      val jsonUpdatedPlaylist =
        """{
           "name": "PlaylistTest",
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
             },
             {
               "trackId": """" + trackId3 + """",
               "trackRank": 4
             }
           ]
          }"""

      val Some(update) = route(FakeRequest(PUT, "/playlists/" + contentAsString(result))
        .withJsonBody(Json.parse(jsonUpdatedPlaylist))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(update) mustEqual OK

      val Some(playlists) = route(FakeRequest(GET, "/playlists")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(playlists).toString() must contain(""""title":"title4","url":"urlPlaylistControllerTestFindByUser4"""")
    }
  }
}

