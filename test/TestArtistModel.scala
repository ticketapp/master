import java.util.UUID
import models._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.iteratee.Iteratee
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}
import scala.util.{Failure, Success}

class TestArtistModel extends PlaySpec with OneAppPerSuite {

  val appBuilder = new GuiceApplicationBuilder()
  val injector = appBuilder.injector()
  val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
  val utilities = new Utilities
  val trackMethods = new TrackMethods(dbConfProvider, utilities)
  val genreMethods = new GenreMethods(dbConfProvider, utilities)
  val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
  val searchYoutubeTrack = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
  val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTrack,
    trackMethods, utilities)

  "An Artist" must {

    "be saved and deleted in database and return the new id" in {
      val artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>
            foundArtist mustBe Option(artist.copy(id = Some(savedArtist.id.get),
              description = Some("<div class='column large-12'>description</div>")))
            whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
          }
        } finally {
          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    /*"be followed and unfollowed by a user" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          whenReady(artistMethods.followByArtistId(artistMethods.UserArtistRelation("userTestId", savedArtist.id.get)),
            timeout(Span(5, Seconds))) {
            whenReady(artistMethods.isFollowed(IdentityId("userTestId", "oauth2"), savedArtist.id.get)) { isFollowed =>
              isFollowed mustBe true
              whenReady(artistMethods.unfollowByArtistId(artistMethods.UserArtistRelation("userTestId",
                savedArtist.id.get))) { unfollow =>
                unfollow mustBe Success(1)
              }
            }
          }
        } finally {
          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    "not be followed twice" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          whenReady(artistMethods.followByArtistId(artistMethods.UserArtistRelation("userTestId", savedArtist.id.get)),
          timeout(Span(5, Seconds))) {
            whenReady(artistMethods.followByArtistId(artistMethods.UserArtistRelation("userTestId", savedArtist.id.get)),
              timeout(Span(5, Seconds))) {
              case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
              case _ => throw new Exception("follow twice an artist worked !")
            }
          }
        } finally {
          whenReady(artistMethods.unfollowByArtistId(artistMethods.UserArtistRelation("userTestId",
            savedArtist.id.get))) { unfollow =>
            unfollow mustBe Success(1)
            artistMethods.delete(savedArtist.id.get)
          }
        }
      }
    }*/

    "be updated" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          val updatedArtist = artist.copy(id = Option(savedArtist.id.get), name = "updatedName")
          artistMethods.update(updatedArtist)
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe Option(updatedArtist) }
        } finally {
          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    "have his websites updated" in {
      val artist = Artist(None, Option("facebookId2"), "artistTest2", Option("imagePath"), Option("description"),
        "facebookUrl2", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          whenReady(artistMethods.addWebsite(savedArtist.id.get, "normalizedUrl"), timeout(Span(5, Seconds))) { resp =>
            whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>
              foundArtist mustBe Option(artist.copy(id = Option(savedArtist.id.get), websites = Set("website", "normalizedUrl"),
                description = Some("<div class='column large-12'>description</div>")))
            }
          }
        } finally {
          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    "have another website" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val maybeTrack = Option(Track(UUID.randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl")))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        val artistWithId = artist.copy(id = Option(savedArtist.id.get))
        artistMethods.addSoundCloudWebsiteIfMissing(maybeTrack, artistWithId)
        try {
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { artistFound =>
            artistFound mustBe Option(artistWithId.copy(websites = Set("website", "redirecturl"),
              description = Some("<div class='column large-12'>description</div>")))
          }
        } catch {
          case e: Exception => throw e
        } finally {
          whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    /*"get tracks for an artist" in {
      val patternAndArtist = PatternAndArtist("Feu! Chatterton",
        Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
          Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
            "youtube.com/user/feuchatterton", "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"),
          List(),List(),None,None))
      val enumerateTracks = artistMethods.getArtistTracks(patternAndArtist)
      val iteratee = Iteratee.foreach[Set[Track]]{track => println("track = " + track)}
      whenReady(enumerateTracks |>> iteratee, timeout(Span(6, Seconds))) { a=>
          a
      }
    }*/
  }
}
