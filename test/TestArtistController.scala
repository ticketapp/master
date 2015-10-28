import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class TestArtistController extends PlaySpecification with Mockito with Injectors {
  sequential

  "artist controller" should {

    "create an artist and return tracks found in enumerator" in new Context {
      new WithApplication(application) {
        val artistJson = Json.parse("""{
          "searchPattern": "worakls",
          "artist": {
            "facebookUrl": "worakls",
            "artistName": "worakls",
            "facebookId": "100297159501",
            "imagePath": "jskd",
            "websites": ["hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls","facebook.com/worakls"],
            "genres": [{"name": "rock", "weight": 0}],
            "description": "artist.description"
          }
        }""")
        val Some(result) = route(FakeRequest(POST, "/artists/createArtist")
        .withJsonBody(artistJson))

        status(result) mustEqual OK

        //need to test enumerator
      }
    }

    "find a list of artists" in new Context {
      new WithApplication(application) {
        val Some(artists) = route(FakeRequest(GET, "/artists/since?numberToReturn=" + 10 + "&offset=" + 0))
        contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
      }
    }

    "find a list of artist by containing" in new Context {
      new WithApplication(application) {
        val Some(artists) = route(FakeRequest(GET, "/artists/containing/worakls"))
        contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
      }
    }

    "find one artist by id" in new Context {
      new WithApplication(application) {
        val artistId = await(artistMethods.findAllContaining("worakls")).headOption.get.artist.id
        val Some(artist) = route(FakeRequest(GET, "/artists/byId/" + artistId.get))
        contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
      }
    }

    "find one artist by facebookUrl" in new Context {
      new WithApplication(application) {
        val Some(artist) = route(FakeRequest(GET, "/artists/worakls"))
        contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
      }
    }

    "follow and unfollow an artist by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val Some(response) = route(FakeRequest(POST, "/artists/" + artistId.get + "/followByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response1) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "return an error if an user try to follow an artist twice" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val Some(response) = route(FakeRequest(POST, "/artists/" + artistId.get + "/followByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response) mustEqual CREATED

        val Some(response1) = route(FakeRequest(POST, "/artists/" + artistId.get + "/followByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        status(response1) mustEqual CONFLICT

        val Some(response2) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "follow and unfollow an artist by facebookId" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val Some(response) = route(FakeRequest(POST, "/artists/100297159501/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(response2) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find followed artists" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val Some(response) = route(FakeRequest(POST, "/artists/100297159501/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(artists) = route(FakeRequest(GET, "/artists/followed/")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")

        val Some(response2) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find one followed artist by id" in new Context {
      new WithApplication(application) {
        await(userDAOImpl.save(identity))
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val Some(response) = route(FakeRequest(POST, "/artists/100297159501/followByFacebookId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response) mustEqual CREATED

        val Some(artists) = route(FakeRequest(GET, "/artists/" + artistId.get + "/isFollowed")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        contentAsJson(artists) mustEqual Json.parse("true")

        val Some(response2) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

        status(response2) mustEqual OK

        userDAOImpl.delete(identity.uuid)
      }
    }

    "find tracks by facebookUrl" in new Context {
      new WithApplication(application) {

        val Some(tracks) = route(FakeRequest(GET, "/artists/worakls/tracks?numberToReturn=0&offset=0"))

        status(tracks) mustEqual OK

        contentAsJson(tracks).toString() must contain(""""artistFacebookUrl":"worakls","artistName":"worakls"""")

      }
    }

    "find artist by facebook containing" in new Context {
      new WithApplication(application) {
        val Some(tracks) = route(FakeRequest(GET, "/artists/facebookContaining/worakls?numberToReturn=0&offset=0"))

        status(tracks) mustEqual OK

        contentAsJson(tracks).toString() must contain(""""facebookId":"100297159501","name":"Worakls"""")

      }
    }

    "find events by artistFacebookUrl" in new Context {
      new WithApplication(application) {
        val event = Event(None, None, true, true, "artistController.findEventByFacebookId", None, None, new DateTime(100000000000000L),
          None, 1, None, None, None)
        val passedEvent = Event(None, None, true, true, "artistController.findEventByFacebookIdPassedEvent", None, None, new DateTime(0),
          Option(new DateTime(0)), 1, None, None, None)
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val eventId = await(eventMethods.save(event)).id
        val passedEventId = await(eventMethods.save(passedEvent)).id
        await(artistMethods.saveEventRelation(EventArtistRelation(eventId.get, artistId.get)))
        await(artistMethods.saveEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))
        val Some(response) = route(FakeRequest(GET, "/artists/worakls/events"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"artistController.findEventByFacebookId"""")
        contentAsJson(response).toString must not contain """"name":"artistController.findEventByFacebookIdPassedEvent""""

        await(artistMethods.deleteEventRelation(EventArtistRelation(eventId.get, artistId.get)))
        await(artistMethods.deleteEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))
      }
    }

    "find passed events by artistId" in new Context {
      new WithApplication(application) {
        val event = Event(None, None, true, true, "artistController.findEventByArtistId", None, None, new DateTime(100000000000000L),
          None, 1, None, None, None)
        val passedEvent = Event(None, None, true, true, "artistController.findEventByArtistIdPassedEvent", None, None, new DateTime(0),
          Option(new DateTime(0)), 1, None, None, None)
        val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
        val eventId = await(eventMethods.save(event)).id
        val passedEventId = await(eventMethods.save(passedEvent)).id
        await(artistMethods.saveEventRelation(EventArtistRelation(eventId.get, artistId.get)))
        await(artistMethods.saveEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))
        val Some(response) = route(FakeRequest(GET, "/artists/" + artistId.get + "/passedEvents"))

        status(response) mustEqual OK

        contentAsJson(response).toString must contain(""""name":"artistController.findEventByArtistIdPassedEvent"""")
        contentAsJson(response).toString must not contain """"name":"artistController.findEventByArtistId""""
        await(artistMethods.deleteEventRelation(EventArtistRelation(eventId.get, artistId.get)))
        await(artistMethods.deleteEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))
      }
    }

    "find artists by genre" in new Context {
      new WithApplication(application) {
        val Some(response) = route(FakeRequest(GET, "/genres/rock/artists?numberToReturn=200&offset=0"))

        status(response) mustEqual OK

        contentAsJson(response).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
      }
    }
  }
}