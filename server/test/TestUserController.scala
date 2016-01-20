import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import testsHelper.GlobalApplicationForControllers

import scala.language.postfixOps


class TestUserController extends GlobalApplicationForControllers {
  sequential

  "user controller" should {

    "find the geographicPoint for the user" in {

      val Some(geopoint) = route(
        new FakeRequest("GET", "/attendees/geographicPoint", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "81.220.239.243"))
      contentAsString(geopoint) mustEqual
        """{"as":"AS21502 NC Numericable S.A.","city":"Villeurbanne","country":"France","countryCode":"FR","isp":"Numericable","lat":45.7667,"lon":4.8833,"org":"Numericable","query":"81.220.239.243","region":"V","regionName":"Rhône-Alpes","status":"success","timezone":"Europe/Paris","zip":"69100"}""".stripMargin

    }

    "get removed tracks for an user" in {
      val Some(removedTracks) = route(FakeRequest(GET, "/attendees/tracksRemoved")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(removedTracks) must contain("13894e56-08d1-4c1f-b3e4-466c069d15ed")
    }
  }
}

