import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.EventArtistRelation
import database.MyPostgresDriver.api._
import eventsDomain.{Event, EventWithRelations}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestArtistController extends GlobalApplicationForControllers {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('200', 'name0', 'facebookUrl00');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('300', 'name00', 'facebookUrl000');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(
          true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(ispublic, isactive, name, starttime, endtime) VALUES(
          true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');

        INSERT INTO genres(name, icon) VALUES('genretest0', 'a');

        INSERT INTO artistsgenres(artistid, genreid, weight) VALUES
          ((SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'),
           (SELECT genreid FROM genres WHERE name = 'genretest0'), 1);

        INSERT INTO eventsartists(eventid, artistid) VALUES
          ((SELECT eventId FROM events WHERE name = 'name0'),
           (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));
        INSERT INTO eventsartists(eventid, artistid) VALUES
          ((SELECT eventId FROM events WHERE name = 'eventPassed'),
           (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl0'));
        INSERT INTO eventsartists(eventid, artistid) VALUES
          ((SELECT eventId FROM events WHERE name = 'name0'),
           (SELECT artistid FROM artists WHERE facebookurl = 'facebookUrl00'));
        INSERT INTO eventsartists(eventid, artistid) VALUES
          (2, 300);
        """),
      5.seconds)
  }

  "artist controller" should {

    "create an artist and return tracks found in enumerator" in {
      val uuidPattern = """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""".r

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
      val Some(result) = route(FakeRequest(artistsDomain.routes.ArtistController.createArtist())
        .withJsonBody(artistJson))

      status(result) mustEqual OK

      val uuids = uuidPattern.findAllIn(contentAsString(result)).toSeq

      val uuidsFromDB = Await.result(trackMethods.findAllByArtist(
        artistFacebookUrl = "worakls",
        offset = 0,
        numberToReturn = 100000) map { tracks =>
        tracks map(_.uuid)
      }, 5 seconds)

      val stringUUIDsFromDB = uuidsFromDB.map(_.toString)

      val distinctUUIDs = stringUUIDsFromDB.diff(uuids).toList ::: uuids.diff(stringUUIDsFromDB).toList

      stringUUIDsFromDB mustNotEqual Seq.empty

      distinctUUIDs mustEqual Seq.empty
    }

    "add artist event relation" in {
      val Some(relation) = route(
        FakeRequest(artistsDomain.routes.ArtistController.saveEventRelation(1, 300))
      )
      status(relation) mustEqual OK
    }

    "delete artist event relation" in {
      val Some(relation) = route(
        FakeRequest(artistsDomain.routes.ArtistController.deleteEventRelation(2, 300))
      )
      status(relation) mustEqual OK
    }
    
    "update an artist" in {
      val artistJson = Json.parse("""{
          "id": 300,
          "facebookId": "10029715666",
          "name": "waklssss",
          "imagePath": "jskd",
          "description": "artist.description",
          "facebookUrl": "facebookUrl000",
          "websites": ["hungrymusic.fr", "youtube.com/user/worakls/videos", "twitter.com/worakls","facebook.com/worakls"],
          "hasTracks": false,
          "likes": 1
      }""")
      val Some(result) = route(FakeRequest(artistsDomain.routes.ArtistController.updateArtist())
        .withJsonBody(artistJson))

      status(result) mustEqual OK
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
      val Some(result) = route(FakeRequest(artistsDomain.routes.ArtistController.createArtist())
        .withJsonBody(artistJson))

      status(result) mustEqual CONFLICT
    }

    "find all artists since offset" in {
      val Some(artists) = route(FakeRequest(artistsDomain.routes.ArtistController.artistsSinceOffset(
        numberToReturn = 10,
        offset = 0)))

      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find all artists containing worakls" in {
      val Some(artists) = route(FakeRequest(artistsDomain.routes.ArtistController.findArtistsContaining("worakls")))
      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find one artist by id" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).headOption.get.artist.id
      val Some(artist) = route(FakeRequest(artistsDomain.routes.ArtistController.artist(artistId.get)))
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

    "find artist by facebook containing" in {
      val Some(tracks) = route(FakeRequest(GET, "/artists/facebookContaining/worakls?numberToReturn=0&offset=0"))

      status(tracks) mustEqual OK

      contentAsJson(tracks).toString() must contain(""""facebookId":"100297159501","name":"Worakls"""")
    }

    "find events by artistFacebookUrl" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findByArtist("facebookUrl00")))

      status(response) mustEqual OK

      contentAsJson(response).toString must contain (""""name":"name0"""")
      contentAsJson(response).toString must not contain """"name":"name00""""
    }

    "find passed events by artistId" in {
      val event = EventWithRelations(
        event = Event(
          name = "artistController.findEventByArtistId",
          startTime = new DateTime(100000000000000L)))
      val passedEvent = EventWithRelations(
        event = Event(
          name = "artistController.findEventByArtistIdPassedEvent",
          startTime = new DateTime(0),
          endTime = Option(new DateTime(0))))

      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val eventId = await(eventMethods.save(event)).id
      val passedEventId = await(eventMethods.save(passedEvent)).id
      await(artistMethods.saveEventRelation(EventArtistRelation(eventId.get, artistId.get)))
      await(artistMethods.saveEventRelation(EventArtistRelation(passedEventId.get, artistId.get)))

      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findPassedByArtist(artistId.get)))

      status(response) mustEqual OK

      contentAsJson(response).toString must contain(""""name":"artistController.findEventByArtistIdPassedEvent"""")
      contentAsJson(response).toString must not contain """"name":"artistController.findEventByArtistId""""
    }

    "find artists by genre" in {
      val Some(response) = route(FakeRequest(GET, "/genres/genreTest0/artists?numberToReturn=200&offset=0"))

      status(response) mustEqual OK
      contentAsJson(response).toString() must contain("""{"artist":{"id":100,"name":"name","facebookUrl":"facebookUrl0"""")
    }
  }
}