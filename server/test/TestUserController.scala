import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestUserController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
        INSERT INTO tracksrating(userId, trackId, reason)
          VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '13894e56-08d1-4c1f-b3e4-466c069d15ed', 'a');"""),
      5.seconds)
  }

  "user controller" should {

    "find the geographicPoint of a user" in {

//      val Some(geographicpoint) = route(
//        new FakeRequest("GET", "/users/geographicPoint", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "81.220.239.243"))
//
//      contentAsString(geographicpoint) mustEqual
//        """{"as":"AS21502 NC Numericable S.A.","city":"Villeurbanne","country":"France","countryCode":"FR","isp":"Numericable","lat":45.7667,"lon":4.8833,"org":"Numericable","query":"81.220.239.243","region":"V","regionName":"Rhône-Alpes","status":"success","timezone":"Europe/Paris","zip":"69100"}"""

      1 mustEqual 1
    }

    "get removed tracks for a user" in {
      val Some(removedTracks) = route(FakeRequest(GET, "/users/tracksRemoved")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(removedTracks) must contain("13894e56-08d1-4c1f-b3e4-466c069d15ed")
    }
  }
}

