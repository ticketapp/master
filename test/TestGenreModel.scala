import controllers.DAOException
import org.scalatest.Matchers
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{Artist, Genre}
import models.Genre._
import org.scalatestplus.play._
import Matchers._

class TestGenreModel extends PlaySpec with OneAppPerSuite {

  "A genre" must {

    "saved and deleted" in {
      val genre = Genre(None, "rock", Option("r"))
      save(genre) match {
        case Some(long: Long) => delete(long) mustBe 1
        case _ => throw new Exception("genre could not be saved")
      }
    }

    "save its relation with an artist" in {
      val genre = Genre(None, "rock", Option("r"))
      save(genre) match {
        case Some(genreId: Long) =>
          val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
            "facebookUrlGenre", Set("website"))
          Artist.save(artist) match {
            case None =>
              throw new DAOException("TestArtists, error while saving artist ")
            case Some(artistId: Long) =>

              saveArtistRelation(artistId, genreId) mustBe true
              Artist.findByGenre("rock", 1, 0) should not be empty

              Artist.delete(artistId) mustBe 1
              delete(genreId) mustBe 1
          }
        case _ =>
          throw new Exception("genre could not be saved")
      }
    }

  }
}
