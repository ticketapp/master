import java.util.UUID._

import controllers.DAOException
import org.scalatest.Matchers
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{Artist, Genre, Track}
import models.Genre._
import org.scalatestplus.play._
import Matchers._
import play.api.Logger

import scala.util.{Failure, Success}
import services.Utilities._

class TestGenreModel extends PlaySpec with OneAppPerSuite {

  "A genre" must {

    "saved and deleted" in {
      val genreId = save(Genre(None, "rockadocka", Option("r"))).get
      delete(genreId) mustBe 1
    }

    "save and delete its relation with an artist" in {
      val genre = Genre(None, "rockiyadockia", Option("r"))
      val genreId = save(genre).get
      val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
        "artistFacebookUrlTestGenre", Set("website"))
      val artistId = Artist.save(artist).get

      try {
        saveArtistRelation(artistId, genreId) mustBe true
        Artist.findByGenre("rockiyadockia", 1, 0) should not be empty
        deleteArtistRelation(artistId, genreId) mustBe Success(1)
      } finally {
        delete(genreId)
        Artist.delete(artistId)
      }
    }

    "save and delete its relation with a track" in {
      val genre = Genre(None, "rockerdocker", Option("r"))
      val genreId = save(genre).get
      val trackId = randomUUID
      val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
        "artistFacebookUrlTestGenre2", Set("website"))
      val artistId = Artist.save(artist).get
      Track.save(Track(trackId, "titleTestGenreModel1", "url", 's', "thumbnailUrl", "artistFacebookUrlTestGenre2", "artistName"))

      try {
        saveTrackRelation(trackId, genreId, 5) mustBe Success(true)
        Track.findByGenre("rockerdocker", 1, 0).get should not be empty
        deleteTrackRelation(trackId, genreId) mustBe Success(1)
      } finally {
        Track.delete(trackId)
        Artist.delete(artistId)
        delete(genreId)
      }
    }

    "find all genres for a track" in {
      val artist = Artist(None, Option("facebookIdTestGenreModel"), "artistTest", Option("imagePath"),
        Option("description"), "artistFacebookUrlTestGenreModel", Set("website"))
      val artistId = Artist.save(artist).get
      val trackId = randomUUID
      val track = Track(trackId, "titleTestUserModel", "url2", 's', "thumbnailUrl",
        "artistFacebookUrlTestGenreModel", "artistName")

      val genre = Genre(None, "pilipilipims", None)
      val genreId = save(genre).get

      try {
        Track.save(track)
        saveTrackRelation(trackId, genreId, 50)

        findAllByTrack(trackId) mustBe Seq(genre.copy(genreId = Some(genreId)))

      } finally {
        deleteTrackRelation(trackId, genreId)
        delete(genreId)
        Track.delete(trackId)
        Artist.delete(artistId)
      }
    }
  }
}
