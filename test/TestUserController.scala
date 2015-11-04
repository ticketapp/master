import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.libs.json._
import play.api.mvc.Headers
import play.api.test.FakeRequest
import org.specs2.matcher.Matcher._
import play.api.test.Helpers._
import play.mvc.Http
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global


class TestUserController extends GlobalApplicationForControllers {
  sequential

  "user controller" should {
    "ujnb" in {
//      val Some(events) = route(FakeRequest(GET, "/events?geographicPoint=4.2,4.3&numberToReturn=" + 10 + "&offset=" + 0))
//      contentAsJson(events).toString() must
//        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      implicit val remoteAddress = "81.220.239.243"


      val Some(geopoint) = route(
        FakeRequest(GET, "/users/geographicPoint"/*, remoteAddress = "81.220.239.243"*/))

      contentAsString(geopoint) should contain(""""title":"trackdTest","url":"url"""")

      1 mustEqual 1
    }

    "get removed tracks for an user" in {
      val Some(removedTracks) = route(FakeRequest(GET, "/users/tracksRemoved")
      .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      
      contentAsString(removedTracks) must contain("13894e56-08d1-4c1f-b3e4-466c069d15ed")
    }
//
//    "find the geographicPoint for the user" in {
//      val Some(geopoint) = route(FakeRequest(GET, "/users/geographicPoint")
//        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
//
//      contentAsString(geopoint) should contain(""""title":"trackTest","url":"url"""")
//    }
  }
}

