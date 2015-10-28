import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import org.specs2.mock.Mockito
import play.api.db.evolutions.Evolutions
import play.api.libs.json._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

import scala.language.postfixOps

class TestPlaceController extends PlaySpecification with Mockito with Injectors {
  sequential

  Evolutions.applyEvolutions(databaseApi.database("tests"))

  "place controller" should {

    "create a place" in new Context {
      new WithApplication(application) {
        val place = Place(id= None, name= "placeTest",
          geographicPoint= Option(geographicPointMethods.stringToGeographicPoint("4.2,4.3").get),
          facebookId= Option("111"))
        val jsonPlace = """{ "name": "PlaceTest", "geographicPoint": "4.2,4.3", "facebookId": "111"}"""

        val Some(result) = route(FakeRequest(POST, "/places/create")
          .withJsonBody(Json.parse(jsonPlace))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(result) mustEqual OK
      }
    }

    "find a list of places" in new Context {
      new WithApplication(application) {
        val Some(places) = route(FakeRequest(GET, "/places?geographicPoint=4.2,4.3&numberToReturn=" + 10 + "&offset=" + 0))
        contentAsJson(places).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
      }
    }

    "find a list of place by containing" in new Context {
      new WithApplication(application) {
        val Some(places) = route(FakeRequest(GET, "/places/containing/test"))
        contentAsJson(places).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
      }
    }

    "find one place by id" in new Context {
      new WithApplication(application) {
        val placeId = await(placeMethods.findAllContaining("est")).headOption.get.id
        val Some(place) = route(FakeRequest(GET, "/places/" + placeId.get))
        contentAsJson(place).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
      }
    }

    "follow and unfollow a place by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val placeId = await(placeMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/places/" + placeId.get + "/followByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/places/" + placeId.get + "/unfollowPlaceByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "return an error if a user try to follow a place twice" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val placeId = await(placeMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/places/" + placeId.get + "/followByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/places/" + placeId.get + "/followByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response1) mustEqual CONFLICT

        val Some(response2) = route(FakeRequest(POST, "/places/" + placeId.get + "/unfollowPlaceByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "follow and unfollow a place by facebookId" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val placeId = await(placeMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/places/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response2) = route(FakeRequest(POST, "/places/" + placeId.get + "/unfollowPlaceByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find followed places" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val placeId = await(placeMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/places/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(places) = route(FakeRequest(GET, "/places/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(places).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")

        val Some(response2) = route(FakeRequest(POST, "/places/" + placeId.get + "/unfollowPlaceByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find one followed place by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val placeId = await(placeMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/places/111/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(places) = route(FakeRequest(GET, "/places/" + placeId.get + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(places) mustEqual Json.parse("true")

        val Some(response2) = route(FakeRequest(POST, "/places/" + placeId.get + "/unfollowPlaceByPlaceId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    /*"find places near city" {

    }*/
  }

  Evolutions.cleanupEvolutions(databaseApi.database("tests"))
}
