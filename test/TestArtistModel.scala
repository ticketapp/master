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
import java.util.UUID.randomUUID

class TestArtistModel extends PlaySpec with OneAppPerSuite {

  "An Artist" must {

    val artist = Artist(None, Option("facebookId3"), "artistTest", Option("imagePath"), Option("description"),
      "facebookUrl3", Set("website"))

    "be saved and deleted in database and return the new id" in {
      save(artist) match {
        case None =>
          throw new DAOException("TestArtists, error while saving artist ")
        case Some(artistId: Long) =>
          find(artistId) mustEqual Option(artist.copy(artistId = Some(artistId)))
          delete(artistId) mustBe 1
      }
    }

    "be followed and unfollowed by a user" in {
      followByArtistId("userTestId", 1) shouldBe a[Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByArtistId("userTestId", 1) mustBe Success(1)
    }

    "not be followed twice" in {
      followByArtistId("userTestId", 1) shouldBe a[Success[Option[Long]]]
      followByArtistId("userTestId", 1) shouldBe a[Failure[PSQLException]]
      unfollowByArtistId("userTestId", 1) mustBe Success(1)
    }

    "be updated" in {
      val artistInDatabase = find(1)
      val updatedArtist = artistInDatabase.get.copy(name = "updatedName")
      update(updatedArtist)

      find(1) mustBe Option(updatedArtist)

      find(update(artistInDatabase.get)) mustBe artistInDatabase
    }

    "have his websites updated" in {
      val artistId = Artist.save(artist)

      addWebsite(artistId, "normalizedUrl")

      find(artistId.get) mustBe Option(artist.copy(artistId = artistId, websites = Set("website", "normalizedUrl")))
      delete(artistId.get) mustBe 1
    }

    "have another website" in {
      val maybeTrack = Option(Track(randomUUID.toString, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl", "artistName",
        Option("redirectUrl")))
      val artistId = Artist.save(artist)
      val artistWithId = artist.copy(artistId = artistId)

      addSoundCloudWebsiteIfMissing(maybeTrack, artistWithId)

      val expectedArtist = Option(artistWithId.copy(websites = Set("website", "redirecturl")))

      find(artistId.get) mustBe expectedArtist
      delete(artistId.get) mustBe 1
    }
  }
}
