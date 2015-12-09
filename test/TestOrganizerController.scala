import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import json.JsonHelper._
import organizersDomain.Organizer
import play.api.libs.json._
import play.api.test._
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
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
      val Some(response) = route(FakeRequest(organizersDomain.routes.OrganizerController.followOrganizerByOrganizerId(300))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      Await.result(response, 5.seconds)

        val Some(response1) = route(FakeRequest(organizersDomain.routes.OrganizerController.unfollowOrganizerByOrganizerId(300))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an organizer twice" in {
      val Some(response) = route(FakeRequest(organizersDomain.routes.OrganizerController.followOrganizerByOrganizerId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
   
      status(response) mustEqual CONFLICT
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

    "return true if the organizer is followed else false" in {
      val Some(isOrganizerFollowed) = route(FakeRequest(organizersDomain.routes.OrganizerController.isOrganizerFollowed(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(isOrganizerNotFollowed) = route(FakeRequest(organizersDomain.routes.OrganizerController.isOrganizerFollowed(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(isOrganizerFollowed) mustEqual OK
      status(isOrganizerNotFollowed) mustEqual OK

      contentAsJson(isOrganizerFollowed) mustEqual Json.parse("true")
      contentAsJson(isOrganizerNotFollowed) mustEqual Json.parse("false")
    }

    "get organizers near geoPoint" in {
      val Some(response) = route(
        FakeRequest(organizersDomain.routes.OrganizerController.findOrganizersNear(
          geographicPoint = "5,5",
          numberToReturn = 1000,
          offset = 0))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val organizersJSValue = contentAsJson(response ) \\ "organizer"
      val organizers = organizersJSValue.map(organizer => organizer.as[Organizer])

      status(response) mustEqual OK

      val geoPoints = organizers.map(_.geographicPoint)

      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(latitude = 5.0, longitude = 5.0).get

      val sortedGeoPoint = geoPoints sortBy(point => point.distance(centerPoint))

      geoPoints mustEqual sortedGeoPoint
    }
  }
}

