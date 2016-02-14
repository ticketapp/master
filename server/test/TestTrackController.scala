import java.util.UUID

import artistsDomain.{Artist, ArtistWithWeightedGenres}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestTrackController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('200', 'name', 'facebookUrl00');

        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('02894e56-08d1-4c1f-b3e4-466c069d15ed', 'title', 'url0', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('24894e56-08d1-4c1f-b3e4-466c069d15ed', 'title00', 'url000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('35894e56-08d1-4c1f-b3e4-466c069d15ed', 'title000', 'url0000', 'y', 'thumbnailUrl', 'facebookUrl00', 'artistName0');

        INSERT INTO genres(name, icon) VALUES('genretest0', 'a');

        INSERT INTO tracksgenres(genreid, trackid, weight)
          VALUES((SELECT genreid FROM genres WHERE name = 'genretest0'), '13894e56-08d1-4c1f-b3e4-466c069d15ed', 1);

        INSERT INTO tracksfollowed(userId, trackId)
          VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '02894e56-08d1-4c1f-b3e4-466c069d15ed');

        INSERT INTO tracksrating(userId, trackId, reason)
          VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '13894e56-08d1-4c1f-b3e4-466c069d15ed', 'a');

        INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
          ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
           (SELECT genreid FROM genres WHERE name = 'genretest0'), 1);
        """),
      5.seconds)
  }

  "track controller" should {

    "create a track" in {
      val uuid = UUID.randomUUID()

      val jsonTrack = """{ "trackId": """" + uuid + """",
                           "title": "trackTest",
                           "url": "url",
                           "platform": "y",
                           "thumbnailUrl": "url",
                           "artistFacebookUrl": "artistTrackFacebookUrl",
                           "artistName": "artistTrackTest",
                           "redirectUrl": "url" }"""

      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookIdTestTRackController"), "artistTrackTest",
        Option("imagePath"), Option("description"), "artistTrackFacebookUrl", Set("website")), Vector.empty)
      await(artistMethods.save(artist))
      val Some(result) = route(FakeRequest(tracksDomain.routes.TrackController.createTrack())
        .withJsonBody(Json.parse(jsonTrack))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }

    "find a list of tracks by artist" in {
      val Some(tracks) = route(
        FakeRequest(tracksDomain.routes.TrackController.findAllByArtist("facebookUrl0", 1000, 0)))
      contentAsJson(tracks).toString() must contain(""""uuid":"13894e56-08d1-4c1f-b3e4-466c069d15ed","title":"title0"""")
    }

    "follow and unfollow a track by id" in {
      val Some(response) = route(
        FakeRequest(tracksDomain.routes.TrackController.followTrack("13894e56-08d1-4c1f-b3e4-466c069d15ed"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      Await.result(response, 5.seconds)

      val Some(response1) = route(FakeRequest(tracksDomain.routes.TrackController.unfollowTrack("13894e56-08d1-4c1f-b3e4-466c069d15ed"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow a track already followed" in {
      val Some(response) = route(
        FakeRequest(tracksDomain.routes.TrackController.followTrack("02894e56-08d1-4c1f-b3e4-466c069d15ed"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CONFLICT
    }

    "find followed tracks" in {
      val Some(tracks) = route(FakeRequest(tracksDomain.routes.TrackController.getFollowedTracks())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(tracks) must contain(""""uuid":"02894e56-08d1-4c1f-b3e4-466c069d15ed","title":"title"""")
      contentAsString(tracks) must contain("""{"id":1,"name":"genretest0","icon":"a"}""")
    }

    "return true if the track is followed" in {
      val Some(tracks) = route(FakeRequest(tracksDomain.routes.TrackController.isTrackFollowed("02894e56-08d1-4c1f-b3e4-466c069d15ed"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(tracks) mustEqual OK

      contentAsJson(tracks) mustEqual Json.parse("true")
    }

    "find tracks by facebookUrl" in {
      val Some(tracks) = route(FakeRequest(
        tracksDomain.routes.TrackController.findAllByArtist(facebookUrl = "facebookUrl0", numberToReturn = 0, offset = 0)))

      status(tracks) mustEqual OK

      contentAsString(tracks) must contain("""02894e56-08d1-4c1f-b3e4-466c069d15ed""")
      contentAsString(tracks) must contain("""13894e56-08d1-4c1f-b3e4-466c069d15ed""")
    }
  }
}

