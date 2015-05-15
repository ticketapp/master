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
      followByArtistId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByArtistId("userTestId", 1) mustBe 1
    }

    "not be followed twice" in {
      followByArtistId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      followByArtistId("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollowByArtistId("userTestId", 1) mustBe 1
    }

    "be updated" in {
      val artistInDatabase = find(1)
      val updatedArtist = artistInDatabase.get.copy(name="updatedName")
      update(updatedArtist)

      find(1) mustBe Option(updatedArtist)

      find(update(artistInDatabase.get)) mustBe artistInDatabase
    }
}

  "have another website" in {
    val maybeTrack = Option(Track(None, "title", "url", 'S', "thumbnailUrl", "artistFacebookUrl",
      Option("redirectUrl")))
    val artist = Artist(Option(2), Option("facebookId2"), "artistTest2", Option("imagePath"), Option("description"),
      "facebookUrl2", Set("website1","website2"))

    addSoundCloudWebsiteIfMissing(maybeTrack, artist)

    val expectedArtist = artist.copy(websites = Set("website1", "website2", "redirectUrl"))

    find(artist.artistId.get) mustBe Option(expectedArtist)

    update(artist.copy(websites = Set("website1", "website2")))
  }
}
