import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models.Place
import play.api.libs.json._
import play.api.test.FakeRequest

import scala.language.postfixOps
import json.JsonHelper._


class TestPlaceController extends GlobalApplicationForControllers {
  sequential

  "place controller" should {

    "create a place with an address" in {
      val jsonPlace =
        """{
          |"name": "PlaceTest", "geographicPoint": "4.2,4.3", "facebookId": "111",
          |"address":
          |  {
          |    "street": "tamere",
          |    "city": "tonpere",
          |    "zip": "69000",
          |    "geographicPoint": "5.6,5.4"
          |  }
          |}""".stripMargin

      val Some(result) = route(FakeRequest(controllers.routes.PlaceController.createPlace())
        .withJsonBody(Json.parse(jsonPlace))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }

    "find a list of places" in {
      val Some(places) = route(FakeRequest(GET, "/places?geographicPoint=4.2,4.3&numberToReturn=" + 10 + "&offset=" + 0))
      contentAsJson(places).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
    }

    "find a list of place by containing" in {
      val Some(places) = route(FakeRequest(GET, "/places/containing/test"))
      contentAsJson(places).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
    }

    "find one place by id" in {
      val placeId = await(placeMethods.findAllContaining("est")).headOption.get.place.id
      val Some(place) = route(FakeRequest(GET, "/places/" + placeId.get))
      contentAsJson(place).toString() must contain(""""name":"PlaceTest","facebookId":"111","geographicPoint":"POINT (4.2 4.3)"""")
    }

    "follow and unfollow a place by id" in {
      val Some(response) = route(FakeRequest(controllers.routes.PlaceController.followPlaceByPlaceId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(controllers.routes.PlaceController.unfollowPlaceByPlaceId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response1) mustEqual OK
    }

    "return an error if a user try to follow a place twice" in {
      val Some(response) = route(FakeRequest(controllers.routes.PlaceController.followPlaceByPlaceId(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response2) = route(FakeRequest(controllers.routes.PlaceController.followPlaceByPlaceId(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response2) mustEqual CONFLICT
    }

    "follow by facebookId" in {
      val Some(response) = route(FakeRequest(controllers.routes.PlaceController.followPlaceByFacebookId("facebookIdTestFollowController2"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed places" in {
      val Some(places) = route(FakeRequest(controllers.routes.PlaceController.getFollowedPlaces())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(places) must contain(""""name":"testId4BecauseThereIsTRANSBORDEUR"""")
    }

    "return true if isFollowed else false" in {
      val Some(boolean) = route(FakeRequest(controllers.routes.PlaceController.isPlaceFollowed(4))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(boolean) mustEqual Json.parse("true")

      val Some(boolean2) = route(FakeRequest(controllers.routes.PlaceController.isPlaceFollowed(3))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(boolean2) mustEqual Json.parse("false")
    }


    "get places near geoPoint" in {
      val Some(response) = route(FakeRequest(controllers.routes.PlaceController.places(
        offset = 0,
        numberToReturn = 1000,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val placesJSValue = contentAsJson(response ) \\ "place"
      val places = placesJSValue.map(place => place.as[Place])

      status(response) mustEqual OK
      val geoPoints = places.map(_.geographicPoint)

      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(5.0, 5.0).get
      val sortedGeoPoint = geoPoints.flatten sortBy(point => point.distance(centerPoint))
      val numberOfEmptyValuesRemoved = geoPoints.size - sortedGeoPoint.size
      val sortedGeoPointPlusEmptyValues = (sortedGeoPoint map Option.apply) ++ List.fill(numberOfEmptyValuesRemoved)(None)

      geoPoints mustEqual sortedGeoPointPlusEmptyValues
    }
  }
}
