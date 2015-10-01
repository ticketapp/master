import java.util.UUID._

import controllers.DAOException
import fillDatabase.InsertGenres
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

    "be saved and deleted" in {
      val genreId = save(Genre(None, "rockadocka", "r")).get
      delete(genreId) mustBe 1
    }

    "not be created if empty" in {
      Genre(None, "abc")
      an [java.lang.IllegalArgumentException] should be thrownBy Option(Genre(None, ""))
    }

    "return genreId if try to save existing genre" in {
      val genreId = save(Genre(None, "rockadocka")).get
      val genreId2 = save(Genre(None, "rockadocka")).get
      try {
        assert(genreId > 0)
        genreId mustBe genreId2
      } finally {
        delete(genreId)
        delete(genreId2)
      }
    }

    "save and delete its relation with an artist" in {
      val genre = Genre(None, "rockiyadockia")
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
      val genre = Genre(None, "rockerdocker")
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

      val genre = Genre(None, "pilipilipims")
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

    "find over genres in genre set" in {

      val genreId1 = save(Genre(None, "ragga", "g")).get
      val genreId2 = save(Genre(None, "punk chrétien", "r")).get
      val genreId3 = save(Genre(None, "rabiz", "m")).get
      val genreId4 = save(Genre(None, "rapcore", "h")).get
      val genreId5 = save(Genre(None, "rave", "e")).get
      val genreId6 = save(Genre(None, "rhythm and blues", "j")).get
      val genreId7 = save(Genre(None, "quiet storm", "s")).get
      val genreId8 = save(Genre(None, "punto guajiro", "l")).get
      val genreId9 = save(Genre(None, "Ballade", "c")).get

      val genres = Seq(Genre(None, "ragga"),
        Genre(None, "punk chrétien"),
        Genre(None, "rabiz"),
        Genre(None, "rapcore"),
        Genre(None, "rave"),
        Genre(None, "rhythm and blues"),
        Genre(None, "quiet storm"),
        Genre(None, "punto guajiro"),
        Genre(None, "Ballade")
      )

      val expectedGenres = Seq(Genre(None, "reggae"),
        Genre(None, "rock"),
        Genre(None, "musiques du monde"),
        Genre(None, "hip-hop"),
        Genre(None, "electro"),
        Genre(None, "jazz"),
        Genre(None, "classique"),
        Genre(None, "musiques latines"),
        Genre(None, "chanson")
      )

      val allGenres = Seq(Genre(None, "reggae"),
        Genre(None, "rock"),
        Genre(None, "musiques du monde"),
        Genre(None, "hip-hop"),
        Genre(None, "electro"),
        Genre(None, "jazz"),
        Genre(None, "classique"),
        Genre(None, "musiques latines"),
        Genre(None, "chanson"),
        Genre(None, "ragga"),
        Genre(None, "punk chrétien"),
        Genre(None, "rabiz"),
        Genre(None, "rapcore"),
        Genre(None, "rave"),
        Genre(None, "rhythm and blues"),
        Genre(None, "quiet storm"),
        Genre(None, "punto guajiro"),
        Genre(None, "Ballade"))

      try {
        Genre.findOverGenres(genres) mustBe expectedGenres
        Genre.findOverGenres(genres) ++ genres mustBe allGenres
      } finally {
        delete(genreId1)
        delete(genreId2)
        delete(genreId3)
        delete(genreId4)
        delete(genreId5)
        delete(genreId6)
        delete(genreId7)
        delete(genreId8)
        delete(genreId9)
      }
    }
  }
}
