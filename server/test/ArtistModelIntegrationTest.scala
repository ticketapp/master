import java.util.UUID

import artistsDomain.{Artist, ArtistWithWeightedGenres}
import database.EventArtistRelation
import database.MyPostgresDriver.api._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.Json
import testsHelper.GlobalApplicationForModelsIntegration
import tracksDomain.Track

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ArtistModelIntegrationTest extends GlobalApplicationForModelsIntegration {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO artists(artistid, name, facebookurl, facebookId) VALUES('200', 'name0', 'facebookUrl00', 'testFindIdByFacebookId');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('300', 'deleted', 'facebookUrlDeleted');
        INSERT INTO artists(artistid, name, facebookurl) VALUES('400', 'name01', 'facebookUrl01');

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
        """),
      5.seconds)
  }

  "An Artist" must {

    "be saved in database and return the new artist" in {
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
          Option("description"), "facebookUrl", Set("website")),
        genres = Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>

        val expectedArtist = artist.artist.copy(
            id = Some(savedArtist.id.get),
            description = Some("<div class='column large-12'>description</div>"))

        savedArtist mustBe expectedArtist
      }
    }

    "be deleted in database" in {
      whenReady(artistMethods.delete(300), timeout(Span(5, Seconds))) {

        _ mustBe 1
      }
    }

    "be found in database" in {
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest",
        Option("imagePath"), Option("description"), "facebookUrl", Set("website")),
        genres = Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>

          val expectedArtist = ArtistWithWeightedGenres(
            artist = artist.artist.copy(
              id = Some(savedArtist.id.get),
              description = Some("<div class='column large-12'>description</div>")),
            genres = Vector.empty)

          foundArtist mustBe Option(expectedArtist)
        }
      }
    }

    "return None when trying to save an already existent artist" in {
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, None, "artistTestSaveOrReturnNoneIfDuplicate", None, None,
          "facebookUrlTestSaveOrReturnNoneIfDuplicate", Set("website")),
        genres = Vector.empty)

      whenReady(artistMethods.saveOrReturnNoneIfDuplicate(artist), timeout(Span(5, Seconds))) { maybeSavedArtist =>
        val savedArtistId = maybeSavedArtist.get.id.get

        val expectedArtist = artist.artist.copy(id = Some(savedArtistId))

        maybeSavedArtist mustBe Option(expectedArtist)

        whenReady(artistMethods.find(savedArtistId), timeout(Span(5, Seconds))) { foundArtist =>

          foundArtist mustBe Option(ArtistWithWeightedGenres(artist = expectedArtist))

          whenReady(artistMethods.saveOrReturnNoneIfDuplicate(artist), timeout(Span(5, Seconds))) { savedArtist1 =>
            savedArtist1 mustBe None
          }
        }
      }
    }

    "all be found" in {
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, None, "facebookUrlAllBeFound", None, None, "facebookUrlAllBeFound", Set.empty),
        genres = Vector.empty)
      val artist2 = ArtistWithWeightedGenres(
        artist = Artist(None, None, "facebookUrlAllBeFound2", None, None, "facebookUrlAllBeFound2", Set.empty),
        genres = Vector.empty)

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.save(artist2), timeout(Span(5, Seconds))) { savedArtist2 =>
          whenReady(artistMethods.findSinceOffset(numberToReturn = 100000, offset = 0), timeout(Span(5, Seconds))) { foundArtists =>

            val expectedArtist1 = ArtistWithWeightedGenres(savedArtist, Seq.empty)
            val expectedArtist2 = ArtistWithWeightedGenres(savedArtist2, Seq.empty)

            foundArtists must contain allOf (expectedArtist1, expectedArtist2)
          }
        }
      }
    }

    "save its relation with an event" in {
      whenReady(artistMethods.saveEventRelation(EventArtistRelation(eventId = 1L, artistId =  4L)),
        timeout(Span(5, Seconds))) { result =>

        result mustBe 1
      }
    }

    "save its relation with an event, be found by this event and delete this relation" in {
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, None, "saveAndDeleteEventRelation", None, None, "saveAndDeleteEventRelation", Set.empty),
        genres = Vector.empty)

      whenReady(artistMethods.saveWithEventRelation(artist, 1L), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.findAllByEvent(eventId = 1L), timeout(Span(5, Seconds))) { artistsFound =>

          artistsFound should contain(ArtistWithWeightedGenres(savedArtist, Vector.empty))
        }

        whenReady(artistMethods.deleteEventRelation(EventArtistRelation(1L, savedArtist.id.get)),
          timeout(Span(5, Seconds))) { result =>

          result mustBe 1
        }
      }
    }

    "be updated" in {
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId4"), "artistTest4", Option("imagePath"),
        Option("description"), "facebookUrl4", Set("website")), Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        val updatedArtist = savedArtist.copy(id = Option(savedArtist.id.get), name = "updatedName")
        whenReady(artistMethods.update(updatedArtist), timeout(Span(5, Seconds))) { resp =>
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) {

            _ mustBe Option(ArtistWithWeightedGenres(updatedArtist, Vector.empty))
          }
        }
      }
    }

    "have his websites updated" in {
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId5"), "artistTest5", Option("imagePath"), Option("description"),
        "facebookUrl5", Set("website")), Vector.empty)

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addWebsite(savedArtist.id.get, "normalizedUrl"), timeout(Span(5, Seconds))) { response =>
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>

            val expectedArtist = ArtistWithWeightedGenres(
              foundArtist.get.artist.copy(
                id = Option(savedArtist.id.get),
                websites = Set("website", "normalizedUrl"),
                description = Some("<div class='column large-12'>description</div>")),
              Vector.empty)

            response mustBe 1
            foundArtist mustBe Option(expectedArtist)
          }
        }
      }
    }

    "have his websites updated without duplicates" in {
      whenReady(artistMethods.addWebsite(artistId = 100, normalizedUrl = "normalizedUrl"), timeout(Span(5, Seconds))) { response =>
        whenReady(artistMethods.addWebsite(artistId = 100, normalizedUrl = "normalizedUrl"), timeout(Span(5, Seconds))) { response2 =>
          whenReady(artistMethods.find(100), timeout(Span(5, Seconds))) { foundArtist =>

            response mustBe 1
            response2 mustBe 1
            foundArtist.get.artist.websites mustBe Set("normalizedUrl")
          }
        }
      }
    }

    "have another website" in {
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId6"), "artistTest6", Option("imagePath"), Option("description"),
        "facebookUrl6", Set("website")), Vector.empty)
      val track = Track(UUID.randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl"))

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addSoundCloudUrlIfMissing(track, savedArtist), timeout(Span(5, Seconds))) { _ =>
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { artistFound =>
            artistFound mustBe Option(ArtistWithWeightedGenres(savedArtist.copy(websites = Set("website", "redirecturl"),
              description = Some("<div class='column large-12'>description</div>")), Vector.empty))
          }
        }
      }
    }

    "save Soundcloud websites for an artist" in {
      val track = Track(UUID.fromString("9a9ca254-0245-4a69-b66c-494f3a0ced3e"), "Toi (Snippet)",
        "https://api.soundcloud.com/tracks/190465678/stream", 's',
        "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg", "worakls", "Worakls",
        Some("http://soundcloud.com/worakls/toi-snippet"))
      val artist = Artist(Option(26.toLong), Option("facebookIdTestArtistModel"), "artistTest",
        Option("imagePath"), Option("description"), "facebookUrl", Set("website"))

      whenReady(artistMethods.addWebsitesFoundOnSoundCloud(track, artist), timeout(Span(6, Seconds))) {
        val expectedWebsites = List("http://www.hungrymusic.fr", "https://www.youtube.com/user/worakls/videos",
          "https://twitter.com/worakls", "https://www.facebook.com/worakls/")

        _ mustBe expectedWebsites
      }
    }

    "find an artist id by facebookId" in {
      whenReady(artistMethods.findIdByFacebookId("testFindIdByFacebookId"), timeout(Span(5, Seconds))) { id =>

        id mustBe Some(200)
      }
    }

    "find an artist id by facebookUrl" in {
      whenReady(artistMethods.findIdByFacebookUrl("facebookUrl0"), timeout(Span(5, Seconds))) { id =>

        id mustBe Some(100)
      }
    }

    "find an artist by facebookUrl" in {
      val expectedArtist = Artist(
        id = Option(200),
        name = "name0",
        facebookId = Some("testFindIdByFacebookId"),
        facebookUrl = "facebookUrl00")
      whenReady(artistMethods.findByFacebookUrl("facebookUrl00"), timeout(Span(5, Seconds))) { artist =>

        artist.get.artist mustBe expectedArtist
      }
    }

    "read a facebook artist in Json" in {
      val jsonArtist = Json.parse("""{"name": "Lino Officiel","id": "208555529263642","category": "Musician/Band",
                                 "link": "https://www.facebook.com/linofficiel/","website": "http://arsenik-shop.com",
                                 "genre": "Hip Hop / Rap","likes": 136379}""")
      val expectedArtist = Artist(None, Some("208555529263642"), "Lino Officiel", None, None, "linofficiel",
        Set("arsenik-shop.com"))

      whenReady(artistMethods.readFacebookArtist(jsonArtist), timeout(Span(5, Seconds))) { artist =>

        artist.get.artist mustBe expectedArtist
      }
    }

    "read a set of facebook artist in Json" in {
      val jsonArtist = Json.parse("""{"data": [{"name": "Lino Officiel","id": "208555529263642","category": "Musician/Band",
                                 "link": "https://www.facebook.com/linofficiel/","website": "http://arsenik-shop.com",
                                 "genre": "Hip Hop / Rap","likes": 136379}]}""")

      val expectedArtist = Artist(None, Some("208555529263642"), "Lino Officiel", None, None, "linofficiel",
        Set("arsenik-shop.com"))

      whenReady(artistMethods.readFacebookArtists(jsonArtist), timeout(Span(5, Seconds))) { artists =>

        artists.head.artist mustBe expectedArtist
      }
    }

    "return hasTracks set to true if he as some tracks and vice-versa" in {
      val track = Track(
        uuid = UUID.randomUUID(),
        title = "hasTracksTest",
        url = "hasTracksTest",
        platform = 'a',
        thumbnailUrl = "hasTracksTest",
        artistFacebookUrl = "facebookUrl0",
        artistName = "hasTracksTest")

      whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { _ =>
        whenReady(artistMethods.find(100), timeout(Span(5, Seconds))) { foundArtist =>

          foundArtist.get.artist.hasTracks mustBe true
        }
      }

      whenReady(artistMethods.find(4), timeout(Span(5, Seconds))) { foundArtist =>

        foundArtist.get.artist.hasTracks mustBe false
      }
    }

    "find all containing pattern" in {
      whenReady(artistMethods.findAllContaining("name0"), timeout(Span(5, Seconds))) { foundArtists =>
        foundArtists map(_.artist.name) should contain allOf("name0", "name01")
        foundArtists map(_.artist.name) should not contain "name"
      }
    }

    /* "get tracks for an artist" in {
      val patternAndArtist = PatternAndArtistWithWeightedGenres(Artist("Feu! Chatterton",
        ArtistWithWeightedGenres(Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
          Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
            "youtube.com/user/feuchatterton", "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"),
          /*List(),List(),*/None,None))
      val enumerateTracks = artistMethods.getArtistTracks(patternAndArtist)
      val iteratee = Iteratee.foreach[Set[Track]]{ track => println("track = " + track) }
      whenReady(enumerateTracks |>> iteratee, timeout(Span(6, Seconds))) { tracks =>
          tracks
      }
    }
    */
  }
}
