import java.util.UUID

import artistsDomain.{ArtistWithWeightedGenres, Artist}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.language.postfixOps


class TestTrackController extends GlobalApplicationForControllers {
  sequential

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
      val Some(response1) = route(FakeRequest(tracksDomain.routes.TrackController.unfollowTrack("13894e56-08d1-4c1f-b3e4-466c069d15ed"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED andThen {
        status(response1) mustEqual OK
      }
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

