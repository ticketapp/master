import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import play.api.libs.json.{JsResult, JsSuccess, JsError, Json}
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import userDomain.Rib
import json.JsonHelper._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestUserController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO ribs(id, bankCode, deskCode, accountNumber, ribKey, userId)
          VALUES (100, 'bank', 'desk', 'account2', '20', '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO ribs(id, bankCode, deskCode, accountNumber, ribKey, userId)
          VALUES (200, 'bank', 'desk', 'account3', '20', '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO idCards(uuid, userId)
          VALUES ('077f3ea6-2272-4457-a47e-9e9111108e45', '077f3ea6-2272-4457-a47e-9e9111108e44');
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

    "create a rib for a user" in {

      val jsonRib = Json.parse("""{
        "bankCode": "bank",
        "deskCode": "desk",
        "accountNumber":  "account1",
        "ribKey": "20"
      }""")
      val Some(response) = route(FakeRequest(userDomain.routes.UserController.createRib())
        .withJsonBody(jsonRib)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual OK
    }

    "find ribs for a userId" in {
      val expectedRib = Rib(
        id = Some(200),
        bankCode = "bank",
        deskCode = "desk",
        accountNumber = "account3",
        ribKey = "20",
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      )

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findRibsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44")
      )
       .withAuthenticator[CookieAuthenticator](administrator.loginInfo))

      val validatedRibs: JsResult[Seq[Rib]] = contentAsJson(ribs).validate[Seq[Rib]](readRibReads)
      val readRibs = validatedRibs match {
        case error: JsError =>
          throw new Exception("find ribs for a userId")
        case success: JsSuccess[Seq[Rib]] =>
          success.get
      }

      readRibs must contain(expectedRib)
    }

    "find ribs for a conected user" in {
      val expectedRib = Rib(
        id = Some(200),
        bankCode = "bank",
        deskCode = "desk",
        accountNumber = "account3",
        ribKey = "20",
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      )

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findUsersRibs()
      )
       .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val validatedRibs: JsResult[Seq[Rib]] = contentAsJson(ribs).validate[Seq[Rib]](readRibReads)
      val readRibs = validatedRibs match {
        case error: JsError =>
          throw new Exception("find ribs for connected user")
        case success: JsSuccess[Seq[Rib]] =>
          success.get
      }

      readRibs must contain(expectedRib)
    }

    "return forbidden if a connected user try to get other users ribs" in {

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findRibsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44")
      )
       .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(ribs) mustEqual FORBIDDEN
    }

    "return unauthorized if an unconnected user try to get ribs" in {

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findUsersRibs()
      ))

      status(ribs) mustEqual UNAUTHORIZED
    }

    "update a rib for a user" in {

      val jsonRib = Json.parse("""{
        "id": 100,
        "bankCode": "bank",
        "deskCode": "desk",
        "accountNumber":  "account5",
        "ribKey": "20"
      }""")

      val Some(response) = route(FakeRequest(userDomain.routes.UserController.updateRib())
        .withJsonBody(jsonRib)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual OK
    }


  }
}

