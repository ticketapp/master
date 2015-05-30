import java.util.Date
import controllers.DAOException
import models.{Track, Artist}
import models.Artist._
import org.postgresql.util.PSQLException
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import securesocial.core.Identity
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import securesocial.core.IdentityId
import scala.util.Success
import scala.util.Failure
import services.Utilities.{UNIQUE_VIOLATION, FOREIGN_KEY_VIOLATION}
import java.util.UUID.randomUUID

class TestArtistModel extends PlaySpec with OneAppPerSuite {

  "An Artist" must {

    "be saved and deleted in database and return the new id" in {
      val artist = Artist(None, Option("facebookIdArtistTest"), "artistTest", Option("imagePath"), Option("description"),
        "facebookUrl", Set("website"))
      save(artist) match {
        case None =>
          throw new DAOException("TestArtists, error while saving artist ")
        case Some(artistId: Long) =>
          find(artistId) mustEqual Option(artist.copy(artistId = Some(artistId)))
          delete(artistId) mustBe 1
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

        find(artistId.get) mustBe Option(artist.copy(artistId = artistId, websites = Set("website", "normalizedUrl")))

      } finally {
        delete(artistId.get)
      }
    }

    "have another website" in {
      val artist = Artist(None, Option("facebookId3"), "artistTest3", Option("imagePath"), Option("description"),
        "facebookUrl3", Set("website"))
      val maybeTrack = Option(Track(randomUUID.toString, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl")))
      val artistId = Artist.save(artist)
      val artistWithId = artist.copy(artistId = artistId)

      addSoundCloudWebsiteIfMissing(maybeTrack, artistWithId)

      try {
        find(artistId.get) mustBe Option(artistWithId.copy(websites = Set("website", "redirecturl")))
      } catch {
        case e:Exception => throw e
      } finally {
        delete(artistId.get) mustBe 1
      }
    }
  }
}
