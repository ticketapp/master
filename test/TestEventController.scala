import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import json.JsonHelper._
import models._
import play.api.libs.json._
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


class TestEventController extends GlobalApplicationForControllers {
  sequential

  "event controller" should {

    "create an event" in {
      val jsonEvent = """{
                         "facebookId": "1111",
                        "name": "EventTest1",
                        "geographicPoint": "4.2,4.3",
                        "description": "desc",
                        "startTime": "2025-11-24 12:00",
                        "endTime": "2115-10-24 12:00",
                        "ageRestriction": 1}"""

      val jsonPassedEvent = """{
                         "facebookId": "11121",
                        "name": "EventPassedTest",
                        "geographicPoint": "4.2,4.3",
                        "description": "desc",
                        "startTime": "2015-10-24 12:00",
                        "endTime": "2015-10-24 16:00",
                        "ageRestriction": 1}"""

      val Some(result) = route(FakeRequest(POST, "/events/create")
        .withJsonBody(Json.parse(jsonEvent))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val Some(result1) = route(FakeRequest(POST, "/events/create")
        .withJsonBody(Json.parse(jsonPassedEvent))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      status(result1) mustEqual OK
    }

    "find a list of events" in {
      val Some(events) = route(FakeRequest(GET, "/events?geographicPoint=4.2,4.3&numberToReturn=" + 10 + "&offset=" + 0))
      contentAsJson(events).toString() must
        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
    }

    "find a list of event by containing" in {
      val Some(events) = route(FakeRequest(GET, "/events/containing/test?geographicPoint=4.2,4.3"))
      contentAsJson(events).toString() must
        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
    }

    "find one event by id" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(event) = route(FakeRequest(GET, "/events/" + eventId))
      contentAsJson(event).toString() must
        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
    }

    "follow and unfollow an event by id" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(response) = route(FakeRequest(POST, "/events/" + eventId + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an event twice" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(response) = route(FakeRequest(POST, "/events/" + eventId+ "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      status(response1) mustEqual CONFLICT

      val Some(response2) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response2) mustEqual OK
    }

    "find followed events" in {
      val Some(events) = route(FakeRequest(controllers.routes.EventController.getFollowedEvents())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(events) mustEqual OK

      contentAsString(events) must contain(""""name":"name0"""")
    }

    "find one followed event by id" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(response) = route(FakeRequest(POST, "/events/" + eventId + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(events) = route(FakeRequest(GET, "/events/" + eventId + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(response2) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(events) mustEqual Json.parse("true")

      status(response) mustEqual CREATED

      status(response2) mustEqual OK
    }

    "create an event by facebookId" in {
      val Some(response) = route(FakeRequest(POST, "/events/create/1190535250961554"))

      status(response) mustEqual OK

      contentAsString(response) must contain("L'OR DU COMMUN / LA MICROFAUNE / 2 LYRICISTS")
    }

    "find events in interval" in {
      val Some(response) = route(FakeRequest(GET, "/events/inInterval/5?geographicPoint=4.2,4.3&offset=0&numberToReturn=100"))

      status(response) mustEqual OK

      contentAsString(response) must
        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      contentAsString(response) must not contain
        """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }

    "find passed events in interval" in {
      val Some(response) = route(FakeRequest(GET, "/events/passedInInterval/50000?geographicPoint=4.2,4.3&offset=0&numberToReturn=100"))

      status(response) mustEqual OK

      contentAsString(response) must
        contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      contentAsString(response) must not contain
        """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }

    "find events by placeId" in {
      val eventId = await(eventMethods.findAllContaining("EventTest1") map (_.head.event.id.get))
      val placeId = await(placeMethods.save(Place(None, "placeTestEvent", Option("123456"), None))).id
      await(placeMethods.saveEventRelation(EventPlaceRelation(eventId, placeId.get)))
      val Some(response) = route(FakeRequest(GET, "/places/" + placeId.get + "/events"))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      contentAsString(response) must not contain
        """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }

    "find passed events by placeId" in {
      val eventId = await(eventMethods.saveFacebookEventByFacebookId("11121")).get.id
      val placeId = await(placeMethods.save(Place(None, "placeTestEvent", Option("123456"), None))).id
      await(placeMethods.saveEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
      val Some(response) = route(FakeRequest(GET, "/places/" + placeId.get + "/passedEvents"))

      status(response) mustEqual OK

      contentAsString(response) must
        contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      contentAsString(response) must not contain
        """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }
    
    "find events by organizerId" in {
      val eventId = await(eventMethods.findAllContaining("EventTest1") map (_.head.event.id.get))
      val organizerId = await(organizerMethods.save(Organizer(None, Option("123456"), "organizerTestEvent", None))).id
      await(organizerMethods.saveEventRelation(EventOrganizerRelation(eventId, organizerId.get)))
      val Some(response) = route(FakeRequest(GET, "/organizers/" + organizerId.get + "/events"))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      contentAsString(response) must not contain """"name":"eventPassed""""
    }

    "find passed events by organizerId" in {
      val Some(response) = route(FakeRequest(GET, "/organizers/" + 1 + "/passedEvents"))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"eventPassed"""")

      contentAsString(response) must not contain """"name":"notPassedEvent""""
    }

    "get events near geoPoint" in {
      val Some(response) = route(FakeRequest(controllers.routes.EventController.events(
        offset = 0,
        numberToReturn = 1000,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val eventsJSValue = contentAsJson(response ) \\ "event"
      val events = eventsJSValue.map(event => event.as[Event])

      status(response) mustEqual OK
      val geoPoints = events.map(_.geographicPoint)

      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(5.0, 5.0).get
      val sortedGeoPoint = geoPoints.flatten sortBy(point => point.distance(centerPoint))
      val numberOfEmptyValuesRemoved = geoPoints.size - sortedGeoPoint.size
      val sortedGeoPointPlusEmptyValues = (sortedGeoPoint map Option.apply) ++ List.fill(numberOfEmptyValuesRemoved)(None)

      geoPoints mustEqual sortedGeoPointPlusEmptyValues
    }
  }
}

