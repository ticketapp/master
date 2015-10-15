import java.util.UUID.randomUUID

import models.{Artist, Track}
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.libs.iteratee.Iteratee

import scala.util.{Failure, Success}

class TestArtistModel extends PlaySpec with OneAppPerSuite {

  "An Artist" must {

    "be saved and deleted in database and return the new id" in {
      val artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))
      val artistId = Artist.save(artist).get
      try {
        find(artistId) mustBe Option(artist.copy(id = Some(artistId),
          description = Some("<div class='column large-12'>description</div>")))
        delete(artistId) mustBe 1
      } finally {
        delete(artistId)
      }
    }

    "be followed and unfollowed by a user" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val artistId = Artist.save(artist).get

      try {
        followByArtistId("userTestId", artistId)
        isFollowed(IdentityId("userTestId", "oauth2"), artistId) mustBe true
        unfollowByArtistId("userTestId", artistId) mustBe Success(1)
      } finally {
        Artist.delete(artistId)
      }
    }

    "not be followed twice" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val artistId = Artist.save(artist).get

      try {
        followByArtistId("userTestId", artistId)
        followByArtistId("userTestId", artistId) match {
          case Failure(psqlException: PSQLException) => psqlException.getSQLState mustBe UNIQUE_VIOLATION
          case _ => throw new Exception("follow twice an artist worked !")
        }
      } finally {
        unfollowByArtistId("userTestId", artistId)
        Artist.delete(artistId)
      }
    }

    "be updated" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val artistId = Artist.save(artist)

      try {
        val updatedArtist = artist.copy(id = artistId, name = "updatedName")
        update(updatedArtist)

        find(artistId.get) mustBe Option(updatedArtist)
      } finally {
        Artist.delete(artistId.get)
      }
    }

    "have his websites updated" in {
      val artist = Artist(None, Option("facebookId2"), "artistTest2", Option("imagePath"), Option("description"),
        "facebookUrl2", Set("website"))
      val artistId = Artist.save(artist)

      try {
        addWebsite(artistId, "normalizedUrl")

        find(artistId.get) mustBe Option(artist.copy(id = artistId, websites = Set("website", "normalizedUrl"),
          description = Some("<div class='column large-12'>description</div>")))

      } finally {
        delete(artistId.get)
      }
    }

    "have another website" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val maybeTrack = Option(Track(randomUUID, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl")))
      val artistId = Artist.save(artist)
      val artistWithId = artist.copy(id = artistId)

      addSoundCloudWebsiteIfMissing(maybeTrack, artistWithId)

      try {
        find(artistId.get) mustBe Option(artistWithId.copy(websites = Set("website", "redirecturl"),
          description = Some("<div class='column large-12'>description</div>")))
      } catch {
        case e:Exception => throw e
      } finally {
        delete(artistId.get) mustBe 1
      }
    }

    "get tracks for an artist" in {
      val patternAndArtist = PatternAndArtist("Feu! Chatterton",
        Artist(Some(236),Some("197919830269754"),"Feu! Chatterton", None ,None , "kjlk",
          Set("soundcloud.com/feu-chatterton", "facebook.com/feu.chatterton", "twitter.com/feuchatterton",
            "youtube.com/user/feuchatterton", "https://www.youtube.com/channel/UCGWpjrgMylyGVRIKQdazrPA"),
          List(),List(),None,None))
      val enumerateTracks = getArtistTracks(patternAndArtist)
      val iteratee = Iteratee.foreach[Set[Track]]{track => println("track = " + track)}

      whenReady(enumerateTracks |>> iteratee, timeout(Span(6, Seconds))) { a=>
          a
      }
    }
  }
}
