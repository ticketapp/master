import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import json.JsonHelper._
import placesDomain.Place
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestPlaceController extends GlobalApplicationForControllers {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO places(name, geographicPoint, facebookId)
          VALUES ('Le transbordeur', ST_GeomFromText('POINT(45.783808 4.860598)', 4326), '117030545096697');
        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(100, 'Test', '776137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711350');
        INSERT INTO places(placeid, name, facebookid, geographicpoint)
          VALUES(300, 'Test1', '666137029786070', '0101000020E6100000ED2B0FD253E446401503249A40711340');
        INSERT INTO places(placeid, name, facebookid)
          VALUES(400, 'testId4BecauseThereIsTRANSBORDEUR', 'facebookIdTestFollowController');
        INSERT INTO places(placeid, name, facebookid) VALUES(600, 'testId5', 'facebookId600');
        INSERT INTO places(placeid, name, facebookid) VALUES(700, 'testId5', 'facebookId700');
        INSERT INTO places(placeid, name, facebookid) VALUES(800, 'testId5', 'facebookId800');
        INSERT INTO places(placeid, name, facebookid) VALUES(900, 'testId900', 'facebookId900');
        INSERT INTO places(placeid, name, facebookid) VALUES(1000, 'testId5', 'facebookId1000');
        INSERT INTO places(placeid, name, facebookid) VALUES(1100, 'testId5', 'facebookId1100');
        INSERT INTO places(placeid, name, facebookid) VALUES(1200, 'testId5', 'facebookId1200');
        INSERT INTO places(placeid, name, facebookid) VALUES(1300, 'testId5', 'facebookId1300');
        INSERT INTO places(placeid, name, facebookid) VALUES(1400, 'testId5', 'facebookId1400');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'notPassedEvent', timestamp '2042-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'notPassedEvent2', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');

        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT placeid FROM places WHERE name = 'Test'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent'), (SELECT placeid FROM places WHERE name = 'Test'));
        INSERT INTO eventsplaces(eventid, placeid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT placeid FROM places WHERE name = 'Test'));

        INSERT INTO eventsplaces(eventid, placeid)
          VALUES(1, 400);

        INSERT INTO placesfollowed(placeid, userid) VALUES (400, '077f3ea6-2272-4457-a47e-9e9111108e44');
        """),
      5.seconds)
  }


  "Place controller" should {

    "create a place with an address" in {
      val jsonPlace =
        """{
          "name": "PlaceTest", "geographicPoint": "4.2,4.3", "facebookId": "111",
          "address":
            {
              "street": "tamere",
              "city": "tonpere",
              "zip": "69000",
              "geographicPoint": "5.6,5.4"
            }
          }"""

      val Some(result) = route(FakeRequest(placesDomain.routes.PlaceController.createPlace())
        .withJsonBody(Json.parse(jsonPlace))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo))

      status(result) mustEqual OK
    }

    "add place event relation" in {
      val Some(relation) = route(
        FakeRequest(placesDomain.routes.PlaceController.saveEventRelation(1, 300))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo)
      )
      status(relation) mustEqual OK
    }

    "delete place event relation" in {
      val Some(relation) = route(
        FakeRequest(placesDomain.routes.PlaceController.deleteEventRelation(1, 400))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo)
      )
      status(relation) mustEqual OK
    }

    "find a list of places" in {
      val Some(places) = route(
        FakeRequest(placesDomain.routes.PlaceController.places(geographicPoint = "5,4", numberToReturn = 10000, offset = 0)))

      contentAsString(places) must contain(""""name":"PlaceTest","facebookId":"111"""")
    }

    "find a list of places containing" in {
      val Some(places) = route(FakeRequest(GET, "/places/containing/test"))

      contentAsString(places) must contain(""""name":"Test","facebookId":"776137029786070"""")
    }

    "find one place by id" in {
      val Some(place) = route(FakeRequest(GET, "/places/" + 900))

      contentAsString(place) must contain(""""name":"testId900","facebookId":"facebookId900"""")
    }

    "follow and unfollow a place by id" in {
      val Some(response) = route(FakeRequest(placesDomain.routes.PlaceController.followByPlaceId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response1) = route(FakeRequest(placesDomain.routes.PlaceController.unfollowByPlaceId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
      status(response1) mustEqual OK
    }

    "return an error if a user try to follow a place twice" in {
      val Some(response) = route(FakeRequest(placesDomain.routes.PlaceController.followByPlaceId(400))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CONFLICT
    }

    "follow by facebookId" in {
      val Some(response) = route(FakeRequest(placesDomain.routes.PlaceController.followByFacebookId("facebookId600"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed places" in {
      val Some(places) = route(FakeRequest(placesDomain.routes.PlaceController.findFollowed())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(places) must contain(""""name":"testId4BecauseThereIsTRANSBORDEUR"""")
    }

    "return true if isFollowed else false" in {
      val Some(boolean) = route(FakeRequest(placesDomain.routes.PlaceController.isFollowed(400))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(boolean2) = route(FakeRequest(placesDomain.routes.PlaceController.isFollowed(3))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(boolean) mustEqual Json.parse("true")
      contentAsJson(boolean2) mustEqual Json.parse("false")
    }

    "get places near geoPoint" in {
      val Some(response) = route(FakeRequest(placesDomain.routes.PlaceController.places(
        offset = 0,
        numberToReturn = 1000,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val placesJSValue = contentAsJson(response) \\ "place"
      val places = placesJSValue.map(place => place.as[Place])
      val geoPoints = places.map(_.geographicPoint)
      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(5.0, 5.0).get
      val sortedGeoPoint = geoPoints sortBy(point => point.distance(centerPoint))

      status(response) mustEqual OK
      geoPoints mustEqual sortedGeoPoint
    }

    "return places 12 by 12 (and never the same with the same geographicPoint)" in {
      val Some(response) = route(FakeRequest(placesDomain.routes.PlaceController.places(
        offset = 0,
        numberToReturn = 12,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response2) = route(FakeRequest(placesDomain.routes.PlaceController.places(
        offset = 12,
        numberToReturn = 1,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val placesJSValue = contentAsJson(response) \\ "place"
      val places = placesJSValue.map(place => place.as[Place])

      val placesJSValue2 = contentAsJson(response2) \\ "place"
      val places2 = placesJSValue2.map(place => place.as[Place])

      places.diff(places2) ++ places2.diff(places) should have size 13
    }
  }
}
