import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import json.JsonHelper._
import organizersDomain.Organizer
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestOrganizerController extends GlobalApplicationForControllers {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO organizers(name) VALUES('name0');
        INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
          VALUES(100, 'name1', 'facebookId', '0101000020E6100000ED2B0FD253E446401503249A40711350');
        INSERT INTO organizers(organizerid, name, facebookid, geographicpoint)
          VALUES(300, 'name2', 'facebookId1', '0101000020E6100000ED2B0FD253E446401503249A40711340');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint)
          VALUES(true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'notPassedEvent2', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');

        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'name0'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'eventPassed'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent2'), (SELECT organizerid FROM organizers WHERE name = 'name0'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES(2, 300);

        INSERT INTO organizersfollowed(organizerid, userid)
          VALUES((SELECT organizerId FROM organizers WHERE name = 'name0'), '077f3ea6-2272-4457-a47e-9e9111108e44');
        """),
      5.seconds)
  }

  "organizer controller" should {

    "create an organizer" in {
      val Some(result) = route(FakeRequest(organizersDomain.routes.OrganizerController.create())
        .withJsonBody(Json.parse("""{ "facebookId": 111, "name": "test" }"""))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo))
      val organizer = (contentAsJson(result) \ "organizer").as[Organizer]

      status(result) mustEqual OK
      organizer mustEqual Organizer(id = organizer.id, facebookId = Some("111"), name = "test", verified = false)
    }

    "add organizer event relation" in {
      val Some(relation) = route(
        FakeRequest(organizersDomain.routes.OrganizerController.saveEventRelation(1, 300))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo)
      )
      status(relation) mustEqual OK
    }

    "delete organizer event relation" in {
      val Some(relation) = route(
        FakeRequest(organizersDomain.routes.OrganizerController.deleteEventRelation(2, 300))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo)
      )
      status(relation) mustEqual OK
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
      val Some(organizers) = route(FakeRequest(
        organizersDomain.routes.OrganizerController.findContaining("name0")))

      contentAsJson(organizers).toString() must contain(""""name":"name0"""")
    }

    "follow and unfollow an organizer by id" in {
      val Some(response) = route(FakeRequest(organizersDomain.routes.OrganizerController.followByOrganizerId(300))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      Await.result(response, 5.seconds)

        val Some(response1) = route(FakeRequest(organizersDomain.routes.OrganizerController.unfollowByOrganizerId(300))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an organizer twice" in {
      val Some(response) = route(FakeRequest(organizersDomain.routes.OrganizerController.followByOrganizerId(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
   
      status(response) mustEqual CONFLICT
    }

    "follow an organizer by facebookId" in {
      val Some(response) = route(FakeRequest(
        organizersDomain.routes.OrganizerController.followByFacebookId("facebookId"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed organizers" in {
      val Some(organizers) = route(FakeRequest(GET, "/followedOrganizers")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(organizers) must contain(""""facebookId":"facebookId","name":"name1"""")
    }

    "return true if the organizer is followed else false" in {
      val Some(isOrganizerFollowed) = route(FakeRequest(organizersDomain.routes.OrganizerController.isFollowed(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(isOrganizerNotFollowed) = route(FakeRequest(organizersDomain.routes.OrganizerController.isFollowed(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(isOrganizerFollowed) mustEqual OK
      status(isOrganizerNotFollowed) mustEqual OK

      contentAsJson(isOrganizerFollowed) mustEqual Json.parse("true")
      contentAsJson(isOrganizerNotFollowed) mustEqual Json.parse("false")
    }

    "get organizers near geoPoint" in {
      val Some(response) = route(
        FakeRequest(organizersDomain.routes.OrganizerController.findNear(
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

