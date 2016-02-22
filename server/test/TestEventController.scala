import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.EventPlaceRelation
import database.MyPostgresDriver.api._
import eventsDomain.Event
import json.JsonHelper._
import placesDomain.Place
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class TestEventController extends GlobalApplicationForControllers {

  override def beforeAll() {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint)
          VALUES(100, true, true, 'notPassedEvent', timestamp '2050-08-24 14:00:00',
          '01010000008906CEBE97E346405187156EF9581340');
        INSERT INTO events(eventid, ispublic, isactive, name, starttime, geographicpoint)
          VALUES(666, true, true, 'eventToUpdate', timestamp '2050-08-24 14:00:00',
          '01010000008906CEBE97E346405187156EF9581340');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'inProgressEvent', timestamp '2012-08-24 14:00:00', timestamp '2042-08-24 14:00:00');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime)
          VALUES(true, true, 'passedEvent', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO eventsfollowed(eventid, userid) VALUES(1, '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO organizers(name) VALUES('eventOrganizer');
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'passedEvent'), (SELECT organizerid FROM organizers WHERE name = 'eventOrganizer'));
        INSERT INTO eventsorganizers(eventid, organizerid)
          VALUES((SELECT eventId FROM events WHERE name = 'notPassedEvent'), (SELECT organizerid FROM organizers WHERE name = 'eventOrganizer'));"""),
      2.seconds)
  }

  "Event controller" should {

    "create an event" in {
      val jsonEvent = """{
                        "facebookId": "1111",
                        "name": "EventTest1",
                        "geographicPoint": "4.2,4.3",
                        "description": "desc",
                        "startTime": "2025-11-24 12:00",
                        "endTime": "2115-10-24 12:00",
                        "ageRestriction": 1
                      }"""

      val jsonPassedEvent = """{
                              "facebookId": "11121",
                              "name": "EventPassedTest",
                              "geographicPoint": "4.2,4.3",
                              "description": "desc",
                              "startTime": "2015-10-24 12:00",
                              "endTime": "2015-10-24 16:00",
                              "ageRestriction": 1
                            }"""

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

    "update an event" in {

      /*Event(id: Option[Long] = None,
                 facebookId: Option[String] = None,
                 isPublic: Boolean = true,
                 isActive: Boolean = false,
                 name: String,
                 geographicPoint: Geometry = new GeometryFactory().createPoint(new Coordinate(-84, 30)),
                 description: Option[String] = None,
                 startTime: DateTime,
                 endTime: Option[DateTime] = None,
                 ageRestriction: Int = 16,
                 tariffRange: Option[String] = None,
                 ticketSellers: Option[String] = None,
                 imagePath: Option[String] = None)
                 */
      val jsonEvent = """{
                         "id": 666,
                        "facebookId": "1111666",
                        "isPublic": true,
                        "isActive": true,
                        "name": "EventUpadated",
                        "geographicPoint": "POINT (4.2 4.3)",
                        "description": "desc",
                        "startTime": 1,
                        "endTime": 2,
                        "ageRestriction": 1
                      }"""
      val Some(events) = route(
        FakeRequest(
          eventsDomain.routes.EventController.update())
          .withJsonBody(Json.parse(jsonEvent))
      )

      status(events) mustEqual OK
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

      val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      Await.result(response, 5.seconds)

      status(response) mustEqual CREATED

      status(response1) mustEqual OK
    }

    "return an error if a user tries to follow an event twice" in {
      val eventId = await(eventMethods.findAllContaining("test") map (_.head.event.id.get))
      val Some(response) = route(FakeRequest(POST, "/events/" + eventId+ "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      Await.result(response, 5.seconds)
      val Some(response1) = route(FakeRequest(POST, "/events/" + eventId + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      Await.result(response, 5.seconds)
      val Some(response2) = route(FakeRequest(POST, "/events/" + eventId + "/unfollow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      status(response1) mustEqual CONFLICT

      status(response2) mustEqual OK
    }

    "find followed events" in {
      val Some(events) = route(FakeRequest(eventsDomain.routes.EventController.getFollowedEvents())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(events) mustEqual OK

      contentAsString(events) must contain(""""event":{"id":1""")
    }

    "return true if an event is followed else false" in {
      val Some(result) = route(FakeRequest(eventsDomain.routes.EventController.isEventFollowed(1))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val Some(result2) = route(FakeRequest(eventsDomain.routes.EventController.isEventFollowed(2))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.parse("true")
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

      contentAsString(response) must not contain """"name":"passedEvent""""
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
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findByOrganizer(1)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"notPassedEvent"""")

      contentAsString(response) must not contain(""""name":"passedEvent"""")
    }

    "find passed events by organizerId" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findPassedByOrganizer(1)))

      status(response) mustEqual OK

      contentAsString(response) must contain(""""name":"passedEvent"""")

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

