import java.util.UUID._

import controllers.DAOException
import org.scalatest.Matchers
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{Artist, Genre, Track}
import models.Genre._
import org.scalatestplus.play._
import Matchers._

import scala.util.{Failure, Success}

class TestGenreModel extends PlaySpec with OneAppPerSuite {

  "A genre" must {

    "saved and deleted" in {
      val genre = Genre(None, "rockadocka", Option("r"))
      save(genre) match {
        case Some(long: Long) => delete(long) mustBe 1
        case _ => throw new Exception("genre could not be saved")
      }
    }

    "save and delete its relation with an artist" in {
      val genre = Genre(None, "rockadocka", Option("r"))
      save(genre) match {
        case Some(genreId: Long) =>
          val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
            "facebookUrlGenre", Set("website"))
          Artist.save(artist) match {
            case None =>
              throw new DAOException("TestArtists, error while saving artist ")
            case Some(artistId: Long) =>
              try {

                saveArtistRelation(artistId, genreId) mustBe true
                Artist.findByGenre("rockadocka", 1, 0) should not be empty
                deleteArtistRelation(artistId, genreId) mustBe Success(1)

              } catch {
                case e:Exception => throw e
              } finally {
                Artist.delete(artistId)
                delete(genreId)
              }
          }
        case _ =>
          throw new Exception("genre could not be saved")
      }
    }

    "save and delete its relation with a track" in {
      val genre = Genre(None, "rockadocka", Option("r"))
      save(genre) match {
        case Some(genreId: Long) =>
          val trackId = randomUUID.toString
          val track = Track(trackId, "title", "url", 's', "thumbnailUrl", "artistFacebookUrlTestTrack", "artistName")

          Track.save(track) match {
            case Success(true) =>
              try {

                Genre.saveTrackRelation(trackId, genreId, 5) mustBe true
                Track.findByGenre("rockadocka", 1, 0).get should not be empty
                Genre.deleteTrackRelation(trackId, genreId) mustBe Success(0)

              } finally {
                Track.delete(trackId)
                delete(genreId)
              }
            case _ => throw new Exception
          }
        case _ =>
          throw new Exception("genre could not be saved")
      }
    }
  }
}
