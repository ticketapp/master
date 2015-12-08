import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.{EventOrganizerRelation, EventPlaceRelation}
import eventsDomain.Event
import organizersDomain.Organizer
import placesDomain.Place
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import json.JsonHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


class TestEventController extends GlobalApplicationForControllers {
  sequential

  "Event controller" should {

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

      val Some(result) = route(FakeRequest(eventsDomain.routes.EventController.createEvent())
        .withJsonBody(Json.parse(jsonEvent))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val Some(result1) = route(FakeRequest(eventsDomain.routes.EventController.createEvent())
        .withJsonBody(Json.parse(jsonPassedEvent))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      status(result1) mustEqual OK
    }

    "find a list of events" in {
      val Some(events) = route(
        FakeRequest(
          eventsDomain.routes.EventController.events(geographicPoint = "5,4", numberToReturn = 10000, offset = 0)))

      contentAsJson(events).toString() must
        contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
    }

    "find a list of event by containing" in {
      val Some(events) = route(
        FakeRequest(
          eventsDomain.routes.EventController.findAllContaining(pattern = "test", geographicPoint = "4.2,4.3")))

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

      status(response) mustEqual CREATED andThen {
        val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual OK
      }
    }

    "return an error if an user try to follow an event twice" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(response) = route(FakeRequest(POST, "/events/" + eventId+ "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED andThen {
        val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/follow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual CONFLICT andThen {
          val Some(response2) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
            .withAuthenticator[CookieAuthenticator](identity.loginInfo))

          status(response2) mustEqual OK
        }
      }
    }

    "find followed events" in {
      val Some(events) = route(FakeRequest(eventsDomain.routes.EventController.getFollowedEvents())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(events) mustEqual OK

      contentAsString(events) must contain(""""name":"name0"""")
    }

    "return true if an event is followed else false" in {
      val Some(result) = route(FakeRequest(eventsDomain.routes.EventController.isEventFollowed(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.parse("true")

      val Some(result2) = route(FakeRequest(eventsDomain.routes.EventController.isEventFollowed(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(result2) mustEqual Json.parse("false")
      status(result2) mustEqual OK
    }

    "create an event by facebookId" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.createEventByFacebookId("1190535250961554")))

      status(response) mustEqual OK

      contentAsString(response) must contain("L'OR DU COMMUN / LA MICROFAUNE / 2 LYRICISTS")
    }

    "find events in interval" in {
      val Some(response) = route(
        FakeRequest(eventsDomain.routes.EventController.eventsInHourInterval(
          hourInterval = 500000,
          geographicPoint = "4.2,4.3",
          offset = 0,
          numberToReturn=100)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"inProgressEvent"""")

      contentAsString(response) must not contain """"name":"eventPassed""""
    }

    "find passed events in interval" in {
      val Some(response) =
        route(FakeRequest(eventsDomain.routes.EventController.eventsPassedInHourInterval(
          hourInterval = 50000,
          geographicPoint = "4.2,4.3",
          offset=0,
          numberToReturn= 100)))

      status(response) mustEqual OK

      contentAsString(response) must
        contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      contentAsString(response) must not contain
        """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }

    "find events by placeId" in {
      val eventId = await(eventMethods.findAllContaining("EventTest1") map (_.head.event.id.get))
      val placeId = await(placeMethods.save(Place(
        id = None,
        name = "placeTestEvent",
        facebookId = Option("123456")))).id
      await(placeMethods.saveEventRelation(EventPlaceRelation(eventId, placeId.get)))
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findByPlace(placeId.get)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      contentAsString(response) must not contain
        """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
    }

    "find passed events by placeId" in {
      val eventId = await(eventMethods.saveFacebookEventByFacebookId("11121")).get.id
      val placeId = await(placeMethods.save(Place(None, "placeTestEvent", Option("123456")))).id
      await(placeMethods.saveEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findPassedByPlace(placeId.get)))

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

      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findByOrganizer(organizerId.get)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

      contentAsString(response) must not contain """"name":"eventPassed""""
    }

    "find passed events by organizerId" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findPassedByOrganizer(1)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"eventPassed"""")

      contentAsString(response) must not contain """"name":"notPassedEvent""""
    }

    "get events near geoPoint" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.events(
        offset = 0,
        numberToReturn = 1000,
        geographicPoint = "5,5"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val eventsJSValue = contentAsJson(response ) \\ "event"
      val events = eventsJSValue.map(event => event.as[Event])

      status(response) mustEqual OK
      val geoPoints = events.map(_.geographicPoint)

      val centerPoint = geographicPointMethods.latAndLngToGeographicPoint(5.0, 5.0).get
      val sortedGeoPoint = geoPoints sortBy(point => point.distance(centerPoint))

      geoPoints mustEqual sortedGeoPoint
    }
  }
}

