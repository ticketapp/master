import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import org.postgresql.util.PSQLException
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TestArtistModel extends PlaySpec with OneAppPerSuite with Injectors {

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

    "all be found" in {
      val artist = Artist(None, None, "artistTest0", None, None, "facebookUrl0", Set.empty)
      val artist2 = Artist(None, None, "artistTest00", None, None, "facebookUrl00", Set.empty)
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.save(artist2), timeout(Span(5, Seconds))) { savedArtist2 =>
          try {
            whenReady(artistMethods.findSinceOffset(100000, 0), timeout(Span(5, Seconds))) { foundArtist =>

              foundArtist should contain
                (ArtistWithWeightedGenre(savedArtist, Seq.empty), ArtistWithWeightedGenre(savedArtist2, Seq.empty))

              whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) {
                _ mustBe 1
              }
            }
          } finally {
            artistMethods.delete(savedArtist.id.get)
          }
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
      val artist = Artist(None, Option("facebookId5"), "artistTest5", Option("imagePath"), Option("description"),
        "facebookUrl5", Set("website"))
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addWebsite(savedArtist.id.get, "normalizedUrl"), timeout(Span(5, Seconds))) { response =>

          response mustBe 1

          whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { foundArtist =>

            foundArtist mustBe Option(foundArtist.get.copy(id = Option(savedArtist.id.get), websites = Set("website", "normalizedUrl"),
              description = Some("<div class='column large-12'>description</div>")))

            artistMethods.delete(savedArtist.id.get)
          }
        }
      }
    }

    "have another website" in {
      val artist = Artist(None, Option("facebookId6"), "artistTest6", Option("imagePath"), Option("description"),
        "facebookUrl6", Set("website"))
      val track = Track(UUID.randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl"))

      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(artistMethods.addSoundCloudUrlIfMissing(track, savedArtist), timeout(Span(5, Seconds))) { _ =>
          try {
            whenReady(artistMethods.find(savedArtist.id.get), timeout(Span(5, Seconds))) { artistFound =>
              artistFound mustBe Option(savedArtist.copy(websites = Set("website", "redirecturl"),
                description = Some("<div class='column large-12'>description</div>")))
            }
          } finally {
            whenReady(artistMethods.delete(savedArtist.id.get), timeout(Span(5, Seconds))) {
              _ mustBe 1
            }
          }
        }
      }
    }

    "save soundcloud websites for an artist" in {
      val track = Track(UUID.fromString("9a9ca254-0245-4a69-b66c-494f3a0ced3e"),"Toi (Snippet)",
        "https://api.soundcloud.com/tracks/190465678/stream",'s',
        "https://i1.sndcdn.com/artworks-000106271172-2q3z78-large.jpg","worakls","Worakls",
        Some("http://soundcloud.com/worakls/toi-snippet")/*,None,List()*/)
      val artist = Artist(Option(26.toLong), Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))

      whenReady(artistMethods.addWebsitesFoundOnSoundCloud(track, artist), timeout(Span(6, Seconds))) {

        _ mustBe List("http://www.hungrymusic.fr", "https://www.youtube.com/user/worakls/videos",
          "https://twitter.com/worakls", "https://www.facebook.com/worakls/")
      }
    }

   /* "get tracks for an artist" in {
      val patternAndArtist = PatternAndArtist("Feu! Chatterton",
        Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
          Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
            "youtube.com/user/feuchatterton", "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"),
          /*List(),List(),*/None,None))
      val enumerateTracks = artistMethods.getArtistTracks(patternAndArtist)
      val iteratee = Iteratee.foreach[Set[Track]]{ track => println("track = " + track) }
      whenReady(enumerateTracks |>> iteratee, timeout(Span(6, Seconds))) { tracks =>
          tracks
      }
    }*/
  }
}
