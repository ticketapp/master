import java.util.Date
import controllers.DAOException
import models.Artist
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

    "be able to be saved and deleted in database and return the new id" in {
      save(artist) match {
        case None =>
          throw new DAOException("TestArtists, error while saving artist ")
        case Some(artistId: Long) =>
          find(artistId) mustEqual Option(artist.copy(artistId = Some(artistId)))
          delete(artistId) mustBe 1
      }
    }

    "be able to be followed and unfollowed by a user" in {
      followByArtistId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      isFollowed(IdentityId("userTestId", "oauth2"), 1) mustBe true
      unfollowByArtistId("userTestId", 1) mustBe 1
    }

    "not be able to be followed twice" in {
      followByArtistId("userTestId", 1) shouldBe a [Success[Option[Long]]]
      followByArtistId("userTestId", 1) shouldBe a [Failure[PSQLException]]
      unfollowByArtistId("userTestId", 1) mustBe 1
    }
  }
}
