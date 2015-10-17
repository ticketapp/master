import models.{Genre, GenreMethods}
import org.scalatest.time.{Span, Seconds}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.Utilities
import org.scalatest.concurrent.ScalaFutures._


class TestGenreModel extends PlaySpec with OneAppPerSuite {

  "A genre" must {

    val appBuilder = new GuiceApplicationBuilder()
    val injector = appBuilder.injector()
    val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
    val utilities = new Utilities()
    val genreMethods = new GenreMethods(dbConfProvider, utilities)

    "be saved and deleted" in {
      val genre = Genre(None, "rockadocka", 'r')
      whenReady(genreMethods.save(genre), timeout(Span(5,Seconds))) { genreSaved =>
        whenReady(genreMethods.findById(genreSaved.id.get), timeout(Span(5,Seconds))) { genreFound =>

          genreFound.get mustBe genre.copy(id = Option(genreSaved.id.get))
          whenReady(genreMethods.delete(genreSaved.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "not be created if empty" in {
      Genre(None, "abc")
      an [java.lang.IllegalArgumentException] should be thrownBy Option(Genre(None, ""))
    }

    "return the already existing genre when there was a unique violation" in {
      val genre = Genre(None, "rockadockaa")
      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { genreFound =>
        whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { secondGenreFound =>
          try {
            genreFound mustBe secondGenreFound
          } finally {
            whenReady(genreMethods.delete(genreFound.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
            whenReady(genreMethods.delete(secondGenreFound.id.get), timeout(Span(5, Seconds))) { _ mustBe 0 }
          }
        }
      }
    }

/*    "save and delete its relation with an artist" in {
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

        findAllByTrack(trackId) mustBe Seq(genre.copy(id = Some(genreId)))

      } finally {
        deleteTrackRelation(trackId, genreId)
        delete(genreId)
        Track.delete(trackId)
        Artist.delete(artistId)
      }
    }

    "find over genres in genre set" in {

      val genreId1 = save(Genre(None, "genreTest1", "g")).get
      val genreId2 = save(Genre(None, "genreTest2", "r")).get
      val genreId3 = save(Genre(None, "genreTest3", "m")).get
      val genreId4 = save(Genre(None, "genreTest4", "h")).get
      val genreId5 = save(Genre(None, "genreTest5", "e")).get
      val genreId6 = save(Genre(None, "genreTest6", "j")).get
      val genreId7 = save(Genre(None, "genreTest7", "s")).get
      val genreId8 = save(Genre(None, "genreTest8", "l")).get
      val genreId9 = save(Genre(None, "genreTest9", "c")).get

      val genres = Seq(Genre(None, "genreTest1"),
        Genre(None, "genreTest2"),
        Genre(None, "genreTest3"),
        Genre(None, "genreTest4"),
        Genre(None, "genreTest5"),
        Genre(None, "genreTest6"),
        Genre(None, "genreTest7"),
        Genre(None, "genreTest8"),
        Genre(None, "genreTest9")
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
        Genre(None, "genreTest1"),
        Genre(None, "genreTest2"),
        Genre(None, "genreTest3"),
        Genre(None, "genreTest4"),
        Genre(None, "genreTest5"),
        Genre(None, "genreTest6"),
        Genre(None, "genreTest7"),
        Genre(None, "genreTest8"),
        Genre(None, "genreTest9"))

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
    }*/
  }
}
