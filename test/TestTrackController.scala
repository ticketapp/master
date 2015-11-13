import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import play.api.libs.json._
import play.api.test.FakeRequest

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

      val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, Option("facebookIdTestTRackController"), "artistTrackTest",
        Option("imagePath"), Option("description"), "artistTrackFacebookUrl", Set("website")), Vector.empty)
      await(artistMethods.save(artist))
      val Some(result) = route(FakeRequest(POST, "/tracks/create")
        .withJsonBody(Json.parse(jsonTrack))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }

    "find a list of tracks by artist" in {
      val Some(tracks) = route(FakeRequest(GET, "/tracks?artistFacebookUrl=facebookUrl0&numberToReturn=0&offset=" + 0))
      contentAsJson(tracks).toString() must contain(""""uuid":"13894e56-08d1-4c1f-b3e4-466c069d15ed","title":"title0"""")
    }

    "follow and unfollow a track by id" in {
      val Some(response) = route(FakeRequest(POST, "/tracks/" + "13894e56-08d1-4c1f-b3e4-466c069d15ed" + "/addToFavorites")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(POST, "/tracks/" + "13894e56-08d1-4c1f-b3e4-466c069d15ed" + "/removeFromFavorites")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow a track already followed" in {
      val Some(response) = route(FakeRequest(POST, "/tracks/" + "02894e56-08d1-4c1f-b3e4-466c069d15ed" + "/addToFavorites")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CONFLICT
    }

    "find followed tracks" in {
      val Some(tracks) = route(FakeRequest(GET, "/tracks/favorites")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      println(contentAsString(tracks))
      contentAsString(tracks) must contain(""""uuid":"02894e56-08d1-4c1f-b3e4-466c069d15ed","title":"title"""")
      contentAsString(tracks) must contain("""{"id":1,"name":"genreTest0","icon":"a"}""")
    }

    "return true if the track is followed" in {
      val Some(tracks) = route(FakeRequest(GET, "/tracks/" + "02894e56-08d1-4c1f-b3e4-466c069d15ed" + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(tracks) mustEqual OK

      contentAsJson(tracks) mustEqual Json.parse("true")
    }
  }
}

