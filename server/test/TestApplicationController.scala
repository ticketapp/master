import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import com.mohiva.play.silhouette.test._

import scala.language.postfixOps

class TestApplicationController extends GlobalApplicationForControllers {
  sequential

  "Application controller" should {


    "sign out a user being connected" in {
      val Some(result) = route(FakeRequest(GET, "/signOut").withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual SEE_OTHER
    }

    "return a status unauthorized when a user not connected tries to sign out" in {
      val Some(result) = route(FakeRequest(GET, "/signOut"))

      status(result) mustEqual UNAUTHORIZED
    }

  }
}
