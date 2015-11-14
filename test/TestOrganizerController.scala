import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import json.JsonHelper._
import models.Organizer
import play.api.libs.json._
import play.api.test._

import scala.language.postfixOps


class TestOrganizerController extends GlobalApplicationForControllers {
    sequential

  "organizer controller" should {

    "create an organizer" in {
      val Some(result) = route(FakeRequest(POST, "/organizers/create")
          .withJsonBody(Json.parse("""{ "facebookId": 111, "name": "test" }"""))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val organizer = (contentAsJson(result) \ "organizer").as[Organizer]
      organizer mustEqual Organizer(id = organizer.id, facebookId = Some("111"), name = "test", verified = false)

      status(result) mustEqual OK
    }

    "find a list of organizers" in {
       val Some(organizers) = route(FakeRequest(GET, "/organizers"))

       contentAsJson(organizers).toString() must contain(""""name":"name0"""")
    }

    "find one organizer by id" in {
      val Some(organizer) = route(FakeRequest(GET, "/organizers/" + 1))

      contentAsJson(organizer).toString() must contain(""""name":"name0"""")
    }

    "find a list of organizer containing" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/containing/name0"))

      contentAsJson(organizers).toString() must contain(""""name":"name0"""")
    }

    "follow and unfollow an organizer by id" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(POST, "/organizers/" + 1 + "/unfollowOrganizerByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an organizer twice" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(POST, "/organizers/" + 1 + "/followByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response2) = route(FakeRequest(POST, "/organizers/" + 1 + "/unfollowOrganizerByOrganizerId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual CONFLICT

      status(response2) mustEqual OK
    }

    "follow an organizer by facebookId" in {
      val Some(response) = route(FakeRequest(POST, "/organizers/" + "facebookId" + "/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed organizers" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(organizers) must contain(""""facebookId":"facebookId","name":"name1"""")
    }

    "find one followed organizer by id" in {
      val Some(organizers) = route(FakeRequest(GET, "/organizers/" + 2 + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(organizers) mustEqual OK

      contentAsJson(organizers) mustEqual Json.parse("true")
    }

    "get organizers near geoPoint" in {
      val Some(response) = route(FakeRequest(controllers.routes.OrganizerController.findOrganizersNear(
        geographicPoint = "5,5",
        numberToReturn = 1000,
        offset = 0))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val organizersJSValue = contentAsJson(response ) \\ "organizer"
      val organizers = organizersJSValue.map(organizer => organizer.as[Organizer])

      status(response) mustEqual OK
      val geoPoints = organizers.map(_.geographicPoint)

      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(5.0, 5.0).get
      val sortedGeoPoint = geoPoints.flatten sortBy(point => point.distance(centerPoint))
      val numberOfEmptyValuesRemoved = geoPoints.size - sortedGeoPoint.size
      val sortedGeoPointPlusEmptyValues = (sortedGeoPoint map Option.apply) ++ List.fill(numberOfEmptyValuesRemoved)(None)

      geoPoints mustEqual sortedGeoPointPlusEmptyValues
    }
  }
}

