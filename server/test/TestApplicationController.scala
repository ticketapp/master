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

    "return status unauthorized when a not connected user trie to get admin page" in {
      val Some(result) = route(
        FakeRequest(application.routes.Application.admin())
      )
      status(result) mustEqual UNAUTHORIZED
    }

    "return status unauthorized when a connected user trie to get admin page" in {
      val Some(result) = route(
        FakeRequest(application.routes.Application.admin())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )

      status(result) mustEqual FORBIDDEN
    }

    "return status OK when a connected administrator trie to get admin page" in {
      val Some(result) = route(
        FakeRequest(application.routes.Application.admin())
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo)
      )

      status(result) mustEqual OK
    }

  }
}
