import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import org.specs2.mock.Mockito
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.UserDAOImpl

import scala.language.postfixOps

class TestEventController extends PlaySpecification with Mockito {
  sequential

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities()
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val geographicPointMethods = new GeographicPointMethods(dbConfProvider, utilities)
  val tariffMethods = new TariffMethods(dbConfProvider, utilities)
  val placeMethods = new PlaceMethods(dbConfProvider, geographicPointMethods, utilities)
  val organizerMethods = new OrganizerMethods(dbConfProvider, placeMethods, utilities, geographicPointMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)
  val eventMethods = new EventMethods(dbConfProvider, organizerMethods, placeMethods, artistMethods, tariffMethods,
    geographicPointMethods, utilities)
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

  "event controller" should {

    "create an event" in new Context {
      new WithApplication(application) {

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
    }

    "find a list of events" in new Context {
      new WithApplication(application) {
        val Some(events) = route(FakeRequest(GET, "/events?geographicPoint=4.2,4.3&numberToReturn=" + 10 + "&offset=" + 0))
        contentAsJson(events).toString() must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      }
    }

    "find a list of event by containing" in new Context {
      new WithApplication(application) {
        val Some(events) = route(FakeRequest(GET, "/events/containing/test?geographicPoint=4.2,4.3"))
        contentAsJson(events).toString() must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      }
    }

    "find one event by id" in new Context {
      new WithApplication(application) {
        val eventId = await(eventMethods.findAllContaining("test")).headOption.get.id
        val Some(event) = route(FakeRequest(GET, "/events/" + eventId.get))
        contentAsJson(event).toString() must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
      }
    }

    "follow and unfollow an event by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val eventId = await(eventMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/events/" + eventId.get + "/follow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/events/" + eventId.get + "/unfollow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "return an error if an user try to follow an event twice" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val eventId = await(eventMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/events/" + eventId.get + "/follow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/events/" + eventId.get + "/follow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response1) mustEqual CONFLICT

        val Some(response2) = route(FakeRequest(POST, "/events/" + eventId.get + "/unfollow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }


    "find followed events" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val eventId = await(eventMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/events/" + eventId.get + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(events) = route(FakeRequest(GET, "/events/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(events).toString() must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")

        val Some(response2) = route(FakeRequest(POST, "/events/" + eventId.get + "/unfollow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find one followed event by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val eventId = await(eventMethods.findAllContaining("test")).head.id
        val Some(response) = route(FakeRequest(POST, "/events/" + eventId.get + "/follow")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(events) = route(FakeRequest(GET, "/events/" + eventId.get + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(events) mustEqual Json.parse("true")

        val Some(response2) = route(FakeRequest(POST, "/events/" + eventId.get + "/unfollow")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "create an event by facebookId" in new Context {
      new WithApplication(application) {
        val Some(response) = route(FakeRequest(POST, "/events/create/1190535250961554"))

        status(response) mustEqual OK
        contentAsJson(response).toString must contain("L'OR DU COMMUN / LA MICROFAUNE / 2 LYRICISTS")
      }
    }

    "find events in interval" in new Context {
      new WithApplication(application) {
        val Some(response) = route(FakeRequest(GET, "/events/inInterval/5?geographicPoint=4.2,4.3&offset=0&numberToReturn=100"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
      }
    }

    "find passed events in interval" in new Context {
      new WithApplication(application) {
        val Some(response) = route(FakeRequest(GET, "/events/passedInInterval/50?geographicPoint=4.2,4.3&offset=0&numberToReturn=100"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
      }
    }

    "find events by placeId" in new Context {
      new WithApplication(application) {
        val eventId = await(eventMethods.findAllContaining("EventTest1")).head.id
        val placeId = await(placeMethods.save(Place(None, "placeTestEvent", Option("123456"), None))).id
        await(placeMethods.saveEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
        val Some(response) = route(FakeRequest(GET, "/places/" + placeId.get + "/events"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
        await(placeMethods.deleteEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
      }
    }

    "find passed events by placeId" in new Context {
      new WithApplication(application) {
        val eventId = await(eventMethods.saveFacebookEventByFacebookId("11121")).id
        val placeId = await(placeMethods.save(Place(None, "placeTestEvent", Option("123456"), None))).id
        await(placeMethods.saveEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
        val Some(response) = route(FakeRequest(GET, "/places/" + placeId.get + "/passedEvents"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
        await(placeMethods.deleteEventRelation(EventPlaceRelation(eventId.get, placeId.get)))
      }
    }
    
    "find events by organizerId" in new Context {
      new WithApplication(application) {
        val eventId = await(eventMethods.findAllContaining("EventTest1")).head.id
        val organizerId = await(organizerMethods.save(Organizer(None, Option("123456"), "organizerTestEvent", None))).id
        await(organizerMethods.saveEventRelation(EventOrganizerRelation(eventId.get, organizerId.get)))
        val Some(response) = route(FakeRequest(GET, "/organizers/" + organizerId.get + "/events"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
        await(organizerMethods.deleteEventRelation(EventOrganizerRelation(eventId.get, organizerId.get)))
      }
    }

    "find passed events by organizerId" in new Context {
      new WithApplication(application) {
        val eventId = await(eventMethods.saveFacebookEventByFacebookId("11121")).id
        val organizerId = await(organizerMethods.save(Organizer(None, Option("123456"), "organizerTestEvent", None))).id
        await(organizerMethods.saveEventRelation(EventOrganizerRelation(eventId.get, organizerId.get)))
        val Some(response) = route(FakeRequest(GET, "/organizers/" + organizerId.get + "/passedEvents"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"EventPassedTest","geographicPoint":"POINT (4.2 4.3)","description":"desc"""")
        contentAsJson(response).toString must not contain """"name":"EventTest1","geographicPoint":"POINT (4.2 4.3)","description":"desc""""
        await(organizerMethods.deleteEventRelation(EventOrganizerRelation(eventId.get, organizerId.get)))
      }
    }

    /*
    /events/nearCity/:city
    */
  }
}

