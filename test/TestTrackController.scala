import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import org.scalatest.concurrent.ScalaFutures._
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

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

      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookIdTestTRackController"), "artistTrackTest", Option("imagePath"),
        Option("description"), "artistTrackFacebookUrl", Set("website")), Vector.empty)
      await(artistMethods.save(artist))
      val Some(result) = route(FakeRequest(POST, "/tracks/create")
        .withJsonBody(Json.parse(jsonTrack))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }

    "find a list of tracks by artist" in {
      val Some(tracks) = route(FakeRequest(GET, "/tracks/artistTrackFacebookUrl?numberToReturn=0&offset=" + 0))
      contentAsJson(tracks).toString() must contain(""""title":"trackTest","url":"url","platform":"y","thumbnailUrl":"url","artistFacebookUrl":"artistTrackFacebookUrl"""")
    }
  }
}

