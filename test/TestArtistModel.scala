import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import play.api.libs.iteratee.Iteratee
import scala.concurrent.duration._

import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchSoundCloudTracks, SearchYoutubeTracks, Utilities}
import silhouette.UserDAOImpl
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await

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
  val userDAOImpl = new UserDAOImpl(dbConfProvider)

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

    "be followed and unfollowed by a user" in {
      val artist = Artist(None, Option("facebookId2"), "artistTest2", Option("imagePath"), Option("description"),
        "facebookUrl2", Set("website"))
      val uuid: UUID = UUID.randomUUID()
      val loginInfo: LoginInfo = LoginInfo("providerId1", "providerKey1")
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName1"),
        lastName = Option("lastName1"),
        fullName = Option("fullName1"),
        email = Option("email1"),
        avatarURL = Option("avatarUrl1"))

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(artistMethods.followByArtistId(UserArtistRelation(uuid, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { resp =>
              whenReady(artistMethods.isFollowed(UserArtistRelation(uuid, savedArtist.id.get))) { isFollowed =>

                isFollowed mustBe true

                whenReady(artistMethods.unfollowByArtistId(UserArtistRelation(uuid, savedArtist.id.get))) { result =>

                  result mustBe 1
                }
              }
            }
          } finally {
            artistMethods.delete(savedArtist.id.get)
            userDAOImpl.delete(uuid)
          }
        }
      }
    }

    "not be followed twice" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val uuid: UUID = UUID.randomUUID()
      val loginInfo: LoginInfo = LoginInfo("providerId", "providerKey")
      val user: User = User(
        uuid = uuid,
        loginInfo = loginInfo,
        firstName = Option("firstName"),
        lastName = Option("lastName"),
        fullName = Option("fullName"),
        email = Option("email"),
        avatarURL = Option("avatarUrl"))

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(userDAOImpl.save(user), timeout(Span(5, Seconds))) { savedUser =>
          try {
            whenReady(artistMethods.followByArtistId(UserArtistRelation(uuid, savedArtist.id.get)),
              timeout(Span(5, Seconds))) { firstResp =>
                try {
                  Await.result(artistMethods.followByArtistId(UserArtistRelation(uuid, savedArtist.id.get)), 3 seconds)
                } catch {
                  case e: PSQLException =>

                    e.getSQLState mustBe utilities.UNIQUE_VIOLATION
                }
            }
          } finally {
            whenReady(artistMethods.unfollowByArtistId(UserArtistRelation(uuid,
              savedArtist.id.get))) { result =>

              result mustBe 1

              artistMethods.delete(savedArtist.id.get)
              userDAOImpl.delete(uuid)
            }
          }
        }
      }
    }
    
    "be updated" in {
      val artist = Artist(None, Option("facebookId4"), "artistTest4", Option("imagePath"), Option("description"),
        "facebookUrl4", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          val updatedArtist = savedArtist.copy(id = Option(savedArtist.id.get), name = "updatedName")
          whenReady(artistMethods.update(updatedArtist), timeout(Span(5, Seconds))) { resp =>
            whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe Option(updatedArtist) }
          }
        } finally {
          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    "have his websites updated" in {
      val artist = Artist(None, Option("facebookId4"), "artistTest4", Option("imagePath"), Option("description"),
        "facebookUrl4", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        try {
          whenReady(artistMethods.addWebsite(savedArtist.id.get, "normalizedUrl"), timeout(Span(5, Seconds))) { resp =>

            resp mustBe 1

            whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>
              
              foundArtist mustBe Option(foundArtist.get.copy(id = Option(savedArtist.id.get), websites = Set("website", "normalizedUrl"),
                description = Some("<div class='column large-12'>description</div>")))
            }
          }
        } finally {

          artistMethods.delete(savedArtist.id.get)
        }
      }
    }

    "have another website" in {
      val artist = Artist(None, Option("facebookId5"), "artistTest5", Option("imagePath"), Option("description"),
        "facebookUrl5", Set("website"))
      val maybeTrack = Option(Track(UUID.randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl")))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        val artistWithId = artist.copy(id = Option(savedArtist.id.get))
        artistMethods.addSoundCloudWebsiteIfMissing(maybeTrack, artistWithId)
        try {
          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { artistFound =>
            artistFound mustBe Option(artistFound.get.copy(websites = Set("website", "redirecturl"),
              description = Some("<div class='column large-12'>description</div>")))
          }
        } catch {
          case e: Exception => throw e
        } finally {
          whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "get tracks for an artist" in {
      val patternAndArtist = PatternAndArtist("Feu! Chatterton",
        Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
          Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
            "youtube.com/user/feuchatterton", "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"),
          /*List(),List(),*/None,None))
      val enumerateTracks = artistMethods.getArtistTracks(patternAndArtist)
      val iteratee = Iteratee.foreach[Set[Track]]{ track => println("track = " + track) }
      whenReady(enumerateTracks |>> iteratee, timeout(Span(6, Seconds))) { a=>
          a
      }
    }
  }
}
