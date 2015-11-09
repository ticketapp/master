import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.test.FakeRequest

import scala.language.postfixOps

class TestArtistController extends GlobalApplicationForControllers {
  sequential

  "artist controller" should {

    "create an artist and return tracks found in enumerator" in {
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

    "not create an artist twice and return an error" in {
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

      status(result) mustEqual CONFLICT
    }

    "find a list of artists" in {
      val Some(artists) = route(FakeRequest(GET, "/artists/since?numberToReturn=" + 10 + "&offset=" + 0))
      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find a list of artist by containing" in {
      val Some(artists) = route(FakeRequest(GET, "/artists/containing/worakls"))
      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find one artist by id" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).headOption.get.artist.id
      val Some(artist) = route(FakeRequest(GET, "/artists/byId/" + artistId.get))
      contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find one artist by facebookUrl" in {
      val Some(artist) = route(FakeRequest(GET, "/artists/worakls"))
      contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "follow and unfollow an artist by id" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val Some(response) = route(FakeRequest(POST, "/artists/" + artistId.get + "/followByArtistId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an artist twice" in {
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
    }

    "follow and unfollow an artist by facebookId" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val Some(response) = route(FakeRequest(POST, "/artists/100297159501/followByFacebookId")
      .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response2) = route(FakeRequest(POST, "/artists/" + artistId.get + "/unfollowArtistByArtistId")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response2) mustEqual OK
    }

    "find followed artists" in {
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
    }

    "find one followed artist by id" in {
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
    }

    "find tracks by facebookUrl" in {
      val Some(tracks) = route(FakeRequest(GET, "/artists/worakls/tracks?numberToReturn=0&offset=0"))

      status(tracks) mustEqual OK

      contentAsJson(tracks).toString() must contain(""""artistFacebookUrl":"worakls","artistName":"worakls"""")
    }

    "find artist by facebook containing" in {
      val Some(tracks) = route(FakeRequest(GET, "/artists/facebookContaining/worakls?numberToReturn=0&offset=0"))

      status(tracks) mustEqual OK

      contentAsJson(tracks).toString() must contain(""""facebookId":"100297159501","name":"Worakls"""")
    }

    "find events by artistFacebookUrl" in {
      val Some(response) = route(FakeRequest(GET, "/artists/facebookUrl00/events"))

      status(response) mustEqual OK

      contentAsJson(response).toString must contain (""""name":"name0"""")
      contentAsJson(response).toString must not contain """"name":"name00""""
    }

    "find passed events by artistId" in {
      val event = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "artistController.findEventByArtistId", None,
        None, new DateTime(100000000000000L), None, 1, None, None, None))
      val passedEvent = EventWithRelations(Event(None, None, isPublic = true, isActive = true, "artistController.findEventByArtistIdPassedEvent",
        None, None, new DateTime(0), Option(new DateTime(0)), 1, None, None, None))
      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val eventId = await(eventMethods.save(event)).id
      val passedEventId = await(eventMethods.save(passedEvent)).id
      await(artistMethods.saveEventRelation(EventArtistRelation(eventId.get, artistId.get)))
      await(artistMethods.saveEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))
      val Some(response) = route(FakeRequest(GET, "/artists/" + artistId.get + "/passedEvents"))

      status(response) mustEqual OK

      contentAsJson(response).toString must contain(""""name":"artistController.findEventByArtistIdPassedEvent"""")
      contentAsJson(response).toString must not contain """"name":"artistController.findEventByArtistId""""
    }

    "find artists by genre" in {
      val Some(response) = route(FakeRequest(GET, "/genres/rock/artists?numberToReturn=200&offset=0"))

      status(response) mustEqual OK
      contentAsJson(response).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }
  }
}