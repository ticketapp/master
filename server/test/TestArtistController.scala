import artistsDomain.{Artist, ArtistWithWeightedGenres}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import eventsDomain.EventWithRelations
import play.api.libs.json._
import play.api.test.FakeRequest
import json.JsonHelper._
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
        INSERT INTO artists(artistid, name, facebookurl, facebookId) VALUES(
          '400', 'withFacebookId', 'withFacebookId', 'withFacebookId');

        INSERT INTO events(ispublic, isactive, name, starttime, geographicpoint) VALUES(
          true, true, 'name0', current_timestamp, '01010000000917F2086ECC46409F5912A0A6161540');
        INSERT INTO events(eventId, ispublic, isactive, name, starttime, endtime) VALUES(
          200, true, true, 'eventPassed', timestamp '2012-08-24 14:00:00', timestamp '2012-08-24 14:00:00');
        INSERT INTO events(eventId, ispublic, isactive, name, starttime) VALUES(
          300, true, true, 'notLinkedEvent', timestamp '2012-08-24 14:00:00');

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
        INSERT INTO eventsartists(eventid, artistid) VALUES (200, 300);

        INSERT INTO artistsFollowed(userId, artistId) VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', 300);
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
      val Some(result) = route(FakeRequest(artistsDomain.routes.ArtistController.create())
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
      val Some(relation) = route(FakeRequest(
        artistsDomain.routes.ArtistController.saveEventRelation(eventId = 300, artistId = 300)))

      status(relation) mustEqual OK
    }

    "delete artist event relation" in {
      val Some(relation) = route(FakeRequest(
        artistsDomain.routes.ArtistController.deleteEventRelation(eventId = 200, artistId = 300)))

      status(relation) mustEqual OK
    }
    
    "update an artist" in {
      val artistJson = Json.parse("""{
          "id": 200,
          "facebookId": "10029715666",
          "name": "waklssss",
          "imagePath": "jskd",
          "description": "artist.description",
          "facebookUrl": "Updated",
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
      val Some(result) = route(FakeRequest(artistsDomain.routes.ArtistController.create())
        .withJsonBody(artistJson))

      status(result) mustEqual CONFLICT
    }

    "find all artists since offset" in {
      val Some(artists) = route(FakeRequest(artistsDomain.routes.ArtistController.find(
        numberToReturn = 10,
        offset = 0)))

      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find all artists containing worakls" in {
      val Some(artists) = route(FakeRequest(artistsDomain.routes.ArtistController.findContaining("worakls")))
      contentAsJson(artists).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find one artist by id" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).headOption.get.artist.id
      val Some(artist) = route(FakeRequest(artistsDomain.routes.ArtistController.findById(artistId.get)))
      contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "find one artist by facebookUrl" in {
      val Some(artist) = route(FakeRequest(GET, "/artists/worakls"))
      contentAsJson(artist).toString() must contain(""""facebookId":"100297159501","name":"worakls"""")
    }

    "follow and unfollow an artist by id" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val Some(response) = route(FakeRequest(POST, "/followedArtists/artistId/" + artistId.get)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(DELETE, "/followedArtists/artistId/" + artistId.get)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response1) mustEqual OK
    }

    "return an error if an user try to follow an artist twice" in {
      val artistId = await(artistMethods.findAllContaining("worakls")).head.artist.id
      val Some(response) = route(FakeRequest(POST, "/followedArtists/artistId/" + artistId.get)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      status(response) mustEqual CREATED

      val Some(response1) = route(FakeRequest(POST, "/followedArtists/artistId/" + artistId.get)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      status(response1) mustEqual CONFLICT
    }

    "follow an artist by facebookId" in {
      val Some(response) = route(FakeRequest(artistsDomain.routes.ArtistController.followByFacebookId("withFacebookId"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual CREATED
    }

    "find followed artists" in {
      val Some(artists) = route(FakeRequest(artistsDomain.routes.ArtistController.findFollowed())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val expectedArtist = ArtistWithWeightedGenres(
        artist = Artist(
          id = Some(300),
          facebookId = None,
          name = "name00",
          imagePath = None,
          description = None,
          facebookUrl = "facebookUrl000",
          websites = Set.empty,
          hasTracks = false,
          likes = None,
          country = None))

      contentAsJson(artists).as[Seq[ArtistWithWeightedGenres]] must contain(expectedArtist)
    }

    "return true if is followed" in {
      val Some(isFollowed) = route(FakeRequest(artistsDomain.routes.ArtistController.isFollowed(300))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsJson(isFollowed).as[Boolean] mustEqual true
    }

    "find artist by facebook containing" in {
      val Some(tracks) = route(FakeRequest(GET, "/artists/facebookContaining/worakls?numberToReturn=0&offset=0"))

      status(tracks) mustEqual OK

      contentAsJson(tracks).toString() must contain(""""facebookId":"100297159501","name":"Worakls"""")
    }

    "find events by artistFacebookUrl" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findByArtist("facebookUrl0")))

      status(response) mustEqual OK

      contentAsJson(response).toString must contain (""""name":"name0"""")
      contentAsJson(response).toString must not contain """"name":"name00""""
    }

    "find passed events by artistId" in {
      val Some(response) = route(FakeRequest(eventsDomain.routes.EventController.findPassedByArtist(100)))

      status(response) mustEqual OK

      contentAsJson(response).as[Seq[EventWithRelations]].map(_.event.id) must contain(Some(200))
      contentAsJson(response).toString must not contain """"name":"artistController.findEventByArtistId""""
    }

    "find artists by genre" in {
      val Some(response) = route(FakeRequest(
        artistsDomain.routes.ArtistController.findByGenre(genre = "genreTest0", numberToReturn = 200, offset = 0)))

      status(response) mustEqual OK
      contentAsJson(response).toString() must contain("""{"artist":{"id":100,"name":"name","facebookUrl":"facebookUrl0"""")
    }
  }
}