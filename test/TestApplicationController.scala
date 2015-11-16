import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.libs.json._
import play.api.test.FakeRequest

import scala.language.postfixOps

class TestApplicationController extends GlobalApplicationForControllers {
  sequential

  "Application controller" should {

    "return true if a user get the index page being connected" in {
      val Some(result) = route(FakeRequest(GET, "/").withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      contentAsString(result) must contain("""$root.connected = true""")
    }

    "return false if a user get the index page being not connected" in {
      val Some(result) = route(FakeRequest(GET, "/"))

      status(result) mustEqual OK
      contentAsString(result) must contain("""$root.connected = false""")
    }

    "sign out a user being connected" in {
      val Some(result) = route(FakeRequest(GET, "/signOut").withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual SEE_OTHER
    }

    "return a status unauthorized when a user not connected tries to sign out" in {
      val Some(result) = route(FakeRequest(GET, "/signOut"))

      status(result) mustEqual UNAUTHORIZED
    }

//    val signUpForm =
//      """{
//        |  "firstName": "firstName",
//        |  "lastName": "lastName",
//        |  "email": "email@email.com",
//        |  "password": "password"
//        |}
//      """.stripMargin
//
//    "return a status seeOther when a connected user tries to sign up" in {
//
//      val Some(result) = route(FakeRequest(POST, "/signUp")
//        .withJsonBody(Json.parse(signUpForm))
//        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
//
//      status(result) mustEqual SEE_OTHER
//    }
//
//    "return a status ok when a user not connected tries to sign up" in {
//      val Some(result) = route(FakeRequest(POST, "/signUp")
//        .withJsonBody(Json.parse(signUpForm)))
//
//      status(result) mustEqual OK
//    }
//
//    "return a status badRequest when a user tries to sign up with a wrong form" in {
//      val wrongSignOutForm =
//        """{
//          |  "email": "wrongEmail",
//          |  "password": "password",
//          |  "rememberMe": true
//          |}
//        """.stripMargin
//      val Some(result) = route(FakeRequest(POST, "/signUp")
//        .withJsonBody(Json.parse(wrongSignOutForm))
//        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
//
//      status(result) mustEqual BAD_REQUEST
//    }
  }
}
