import java.util.Date
import controllers.DAOException
import models.Artist.delete
import models.Artist.find
import models.Artist.isFollowed
import models.Artist.save
import models.Artist.update
import models.{Genre, Track, Artist}
import models.Artist._
import org.postgresql.util.PSQLException
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs
import play.api.libs.iteratee.Step.Done
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import java.util.UUID.randomUUID
import play.api.libs.iteratee.{Done, Enumeratee, Enumerator, Iteratee}
import ExecutionContext.Implicits.global

class TestArtistModel extends PlaySpec with OneAppPerSuite {

  "An Artist" must {

    "be saved and deleted in database and return the new id" in {
      val artist = Artist(None, Option("facebookIdTestArtistModel"), "artistTest", Option("imagePath"),
        Option("description"), "facebookUrl", Set("website"))
      val artistId = Artist.save(artist).get
      try {
        find(artistId) mustBe Option(artist.copy(artistId = Some(artistId),
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
        val updatedArtist = artist.copy(artistId = artistId, name = "updatedName")
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

        find(artistId.get) mustBe Option(artist.copy(artistId = artistId, websites = Set("website", "normalizedUrl"),
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
      val artistWithId = artist.copy(artistId = artistId)

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
