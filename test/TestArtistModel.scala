import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.Json

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestArtistModel extends GlobalApplicationForModels {

  "An Artist" must {

    "be saved in database and return the new artist" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(
        artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
          Option("description"), "facebookUrl", Set("website")),
        genres = Vector.empty,
        hasTracks = true)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>

        val expectedArtist = artist.artist.copy(
            id = Some(savedArtist.id.get),
            description = Some("<div class='column large-12'>description</div>"))

        savedArtist mustBe expectedArtist
      }
    }

    "be deleted in database" in {
      whenReady(artistMethods.delete(3), timeout(Span(5, Seconds))) {

        _ mustBe 1
      }
    }

    "be found in database" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(
        artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest",
        Option("imagePath"), Option("description"), "facebookUrl", Set("website")),
        genres = Vector.empty,
        hasTracks = true)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>

          val expectedArtist = ArtistWithWeightedGenresAndHasTrack(
            artist = artist.artist.copy(
              id = Some(savedArtist.id.get),
              description = Some("<div class='column large-12'>description</div>")),
            genres = Vector.empty)

          foundArtist mustBe Option(expectedArtist)
        }
      }
    }

    "return None when trying to save an already existent artist" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(
        artist = Artist(None, None, "artistTestSaveOrReturnNoneIfDuplicate", None, None,
          "facebookUrlTestSaveOrReturnNoneIfDuplicate", Set("website")),
        genres = Vector.empty)

      whenReady(artistMethods.saveOrReturnNoneIfDuplicate(artist), timeout(Span(5, Seconds))) { maybeSavedArtist =>
        val savedArtistId = maybeSavedArtist.get.id.get

        val expectedArtist = artist.artist.copy(id = Some(savedArtistId))

        maybeSavedArtist mustBe Option(expectedArtist)

        whenReady(artistMethods.find(savedArtistId), timeout(Span(5, Seconds))) { foundArtist =>

          foundArtist mustBe Option(ArtistWithWeightedGenresAndHasTrack(artist = expectedArtist))

          whenReady(artistMethods.saveOrReturnNoneIfDuplicate(artist), timeout(Span(5, Seconds))) { savedArtist1 =>
            savedArtist1 mustBe None
          }
        }
      }
    }

    "all be found" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, None, "artistTest0", None, None, "facebookUrl0", Set.empty), Vector.empty)
      val artist2 = ArtistWithWeightedGenresAndHasTrack(Artist(None, None, "artistTest00", None, None, "facebookUrl00", Set.empty), Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.save(artist2), timeout(Span(5, Seconds))) { savedArtist2 =>
          whenReady(artistMethods.findSinceOffset(100000, 0), timeout(Span(5, Seconds))) { foundArtists =>

            foundArtists should contain
              (ArtistWithWeightedGenresAndHasTrack(savedArtist, Seq.empty), ArtistWithWeightedGenresAndHasTrack(savedArtist2, Seq.empty))
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
      val artist = ArtistWithWeightedGenresAndHasTrack(
        artist = Artist(None, None, "saveAndDeleteEventRelation", None, None, "saveAndDeleteEventRelation", Set.empty),
        genres = Vector.empty)

      whenReady(artistMethods.saveWithEventRelation(artist, 1L), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.findAllByEvent(1L), timeout(Span(5, Seconds))) { artistsFound =>

          artistsFound should contain(ArtistWithWeightedGenresAndHasTrack(savedArtist, Vector.empty))
        }

        whenReady(artistMethods.deleteEventRelation(EventArtistRelation(1L, savedArtist.id.get)),
          timeout(Span(5, Seconds))) { result =>

          result mustBe 1
        }
      }
    }

    "be updated" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, Option("facebookId4"), "artistTest4", Option("imagePath"), Option("description"),
        "facebookUrl4", Set("website")), Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        val updatedArtist = savedArtist.copy(id = Option(savedArtist.id.get), name = "updatedName")
        whenReady(artistMethods.update(updatedArtist), timeout(Span(5, Seconds))) { resp =>
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe
            Option(ArtistWithWeightedGenresAndHasTrack(updatedArtist, Vector.empty))
          }
        }
      }
    }

    "have his websites updated" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, Option("facebookId5"), "artistTest5", Option("imagePath"), Option("description"),
        "facebookUrl5", Set("website")), Vector.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addWebsite(savedArtist.id.get, "normalizedUrl"), timeout(Span(5, Seconds))) { response =>

          response mustBe 1

          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>

            foundArtist mustBe
              Option(ArtistWithWeightedGenresAndHasTrack(foundArtist.get.artist.copy(
                id = Option(savedArtist.id.get),
                websites = Set("website", "normalizedUrl"),
                description = Some("<div class='column large-12'>description</div>")), Vector.empty))
          }
        }
      }
    }

    "have his websites updated without duplicates" in {
      whenReady(artistMethods.addWebsite(1, "normalizedUrl"), timeout(Span(5, Seconds))) { response =>

        response mustBe 1

        whenReady(artistMethods.addWebsite(1, "normalizedUrl"), timeout(Span(5, Seconds))) { response2 =>

          response2 mustBe 1

          whenReady(artistMethods.find(1), timeout(Span(5, Seconds))) { foundArtist =>

            foundArtist.get.artist.websites mustBe Set("normalizedUrl")
          }
        }
      }
    }

    "have another website" in {
      val artist = ArtistWithWeightedGenresAndHasTrack(Artist(None, Option("facebookId6"), "artistTest6", Option("imagePath"), Option("description"),
        "facebookUrl6", Set("website")), Vector.empty)
      val track = Track(UUID.randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl"))

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addSoundCloudUrlIfMissing(track, savedArtist), timeout(Span(5, Seconds))) { _ =>
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { artistFound =>
            artistFound mustBe Option(ArtistWithWeightedGenresAndHasTrack(savedArtist.copy(websites = Set("website", "redirecturl"),
              description = Some("<div class='column large-12'>description</div>")), Vector.empty))
          }
        }
      }
    }

    "save soundcloud websites for an artist" in {
      val track = Track(UUID.fromString("9a9ca254-0245-4a69-b66c-494f3a0ced3e"), "Toi (Snippet)",
        "https://api.soundcloud.com/tracks/190465678/stream", 's',
        "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg", "worakls", "Worakls",
        Some("http://soundcloud.com/worakls/toi-snippet"))
      val artist = Artist(Option(26.toLong), Option("facebookIdTestArtistModel"), "artistTest",
        Option("imagePath"), Option("description"), "facebookUrl", Set("website"))

      whenReady(artistMethods.addWebsitesFoundOnSoundCloud(track, artist), timeout(Span(6, Seconds))) {

        _ mustBe List("http://www.hungrymusic.fr", "https://www.youtube.com/user/worakls/videos",
          "https://twitter.com/worakls", "https://www.facebook.com/worakls/")
      }
    }

    "find an artist id by facebookId" in {
      whenReady(artistMethods.findIdByFacebookId("testFindIdByFacebookId"), timeout(Span(5, Seconds))) { id =>
        id mustBe Some(5L)
      }
    }

    "find an artist id by facebookUrl" in {
      whenReady(artistMethods.findIdByFacebookUrl("facebookUrl0"), timeout(Span(5, Seconds))) { id =>
        id mustBe Some(1.toLong)
      }
    }

    "find an artist by facebookUrl" in {
      val expectedArtist = Artist(
        id = Option(2L),
        name = "name0",
        facebookUrl = "facebookUrl00")
      whenReady(artistMethods.findByFacebookUrl("facebookUrl00"), timeout(Span(5, Seconds))) { artist =>
        artist.get.artist mustBe expectedArtist
      }
    }

    "read a facebook artist json" in {
      val jsonArtist = Json.parse("""{"name": "Lino Officiel","id": "208555529263642","category": "Musician/Band",
                                 "link": "https://www.facebook.com/linofficiel/","website": "http://arsenik-shop.com",
                                 "genre": "Hip Hop / Rap","likes": 136379}""")
      whenReady(artistMethods.readFacebookArtist(jsonArtist), timeout(Span(5, Seconds))) { artist =>
        artist.get.artist mustBe Artist(None,Some("208555529263642"),"Lino Officiel",None,None,"linofficiel",
          Set("arsenik-shop.com"),None,None)
      }
    }

    "read a set of facebook artist json" in {
      val jsonArtist = Json.parse("""{"data": [{"name": "Lino Officiel","id": "208555529263642","category": "Musician/Band",
                                 "link": "https://www.facebook.com/linofficiel/","website": "http://arsenik-shop.com",
                                 "genre": "Hip Hop / Rap","likes": 136379}]}""")
      whenReady(artistMethods.readFacebookArtists(jsonArtist), timeout(Span(5, Seconds))) { artists =>
        artists.head.artist mustBe Artist(None,Some("208555529263642"),"Lino Officiel",None,None,"linofficiel",
          Set("arsenik-shop.com"),None,None)
      }
    }

    "return hasTracks set to true if he as some tracks and vice-versa" in {
      whenReady(artistMethods.find(1), timeout(Span(5, Seconds))) { foundArtist =>
        foundArtist.get.hasTracks mustBe true
      }
      whenReady(artistMethods.find(4), timeout(Span(5, Seconds))) { foundArtist =>
        foundArtist.get.hasTracks mustBe false
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
