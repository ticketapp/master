import java.util.UUID

import artistsDomain.{Artist, ArtistWithWeightedGenres}
import database.{ArtistGenreRelation, EventGenreRelation, TrackGenreRelation}
import genresDomain.Genre
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import testsHelper.GlobalApplicationForModels
import tracksDomain.Track


class TestGenreModel extends GlobalApplicationForModels {

  "A genre" must {

    "be saved and deleted" in {
      val genre = Genre(None, "rockadocka", 'r')
      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { genreSaved =>
        whenReady(genreMethods.findById(genreSaved.id.get), timeout(Span(5, Seconds))) { genreFound =>

          genreFound.get mustBe genre.copy(id = Option(genreSaved.id.get))

          whenReady(genreMethods.delete(genreSaved.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
        }
      }
    }

    "be saved in lowerCase" in {
      val genre = Genre(None, "RockadocKayoU", 'r')
      val expectedGenre = Genre(None, "rockadockayou", 'r')

      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { genreSaved =>
        whenReady(genreMethods.findById(genreSaved.id.get), timeout(Span(5, Seconds))) { genreFound =>

          genreFound.get mustBe expectedGenre.copy(id = Option(genreSaved.id.get))
        }
      }
    }

    "find a genre by containing" in {
      val genre = Genre(None, "rockodocki", 'r')

      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { genreSaved =>
        whenReady(genreMethods.findAllContaining("rockodo"), timeout(Span(5, Seconds))) { genreFound =>

          genreFound should contain (genre.copy(id = Option(genreSaved.id.get)))

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
            genreFound mustBe secondGenreFound
        }
      }
    }

    "save and delete its relation with a track" in {
     val genre = Genre(None, "rockerdocker")
     val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
       "artistFacebookUrlTestGenre2", Set("website")), Vector.empty)
     val trackId = UUID.randomUUID
     val track = Track(trackId, "titleTestGenreModel1", "url", 's', "thumbnailUrl", "artistFacebookUrlTestGenre2", "artistName")
     whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { savedGenre =>
       whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
         whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
           whenReady(genreMethods.saveTrackRelation(TrackGenreRelation(trackId, savedGenre.id.get)),
             timeout(Span(5, Seconds))) { trackGenreRelation =>

             trackGenreRelation mustBe 1

             whenReady(trackMethods.findAllByGenre("rockerdocker", 1, 0), timeout(Span(5, Seconds))) { tracksSet =>

               assert(tracksSet.nonEmpty)

               whenReady(genreMethods.deleteTrackRelation(TrackGenreRelation(trackId, savedGenre.id.get)),
                 timeout(Span(5, Seconds))) { _ mustBe 1 }
             }
           }
         }
       }
     }
    }

    "find all genres for a track" in {
      val artist = ArtistWithWeightedGenres(Artist(None, Option("facebookIdTestGenreModel"), "artistTest", Option("imagePath"),
        Option("description"), "artistFacebookUrlTestGenreModel5", Set("website")), Vector.empty)
      val trackId = UUID.randomUUID
      val track = Track(trackId, "titleTestUserModel5", "url13", 's', "thumbnailUrl1",
        "artistFacebookUrlTestGenreModel5", "artistName1")
      val genre = Genre(None, "pilipilipims")
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { savedGenre =>
          whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
            whenReady(genreMethods.saveTrackRelation(TrackGenreRelation(trackId, savedGenre.id.get)),
            timeout(Span(5, Seconds))) { genreTrackRelation =>
              whenReady(genreMethods.findAllByTrack(trackId), timeout(Span(5, Seconds))) { tracksSeq =>

                tracksSeq should contain (genre.copy(id = Some(savedGenre.id.get)))

                whenReady(genreMethods.deleteTrackRelation(TrackGenreRelation(trackId, savedGenre.id.get)),
                  timeout(Span(5, Seconds))) { _ mustBe 1 }
              }
            }
          }
        }
      }
    }

    "save its relation with an event" in {
      whenReady(genreMethods.saveEventRelation(EventGenreRelation(1, 2)), timeout(Span(5, Seconds))) { genreEventRelation =>

        genreEventRelation mustBe 1
      }
    }

    "find a sequence of genres by names" in {
      val expectedGenres = Vector(Genre(Some(1), "genretest0", 'a'), Genre(Some(2), "genretest00", 'a'))
      whenReady(genreMethods.findSequenceByName(Seq("genreTest0", "genreTest00"))) { genres =>

        genres mustBe expectedGenres
      }
    }

    "save, update and delete its relation with an artist" in {
      val genre = Genre(None, "rockiyadockiaaa")
      val genre2 = Genre(None, "rockiyadockiooo")
      val artist = ArtistWithWeightedGenres(
        artist = Artist(None, None, "artistGenreRelationTest", None, None, "artistFacebookUrlTestGenre", Set("website")),
        genres = Vector.empty)

      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { savedGenre =>
        whenReady(genreMethods.save(genre2), timeout(Span(5, Seconds))) { savedGenre2 =>
          whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
            whenReady(genreMethods.saveArtistRelation(ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { artistGenreRelation =>
              whenReady(genreMethods.saveArtistRelation(ArtistGenreRelation(savedArtist.id.get, savedGenre2.id.get)),
                timeout(Span(5, Seconds))) { artistGenreRelation2 =>

                artistGenreRelation mustBe ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get, 0)

                whenReady(genreMethods.saveArtistRelation(ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get)),
                  timeout(Span(5, Seconds))) { artistGenreRelationUpdated =>

                  artistGenreRelationUpdated mustBe ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get, 1)

                  whenReady(artistMethods.findAllByGenre(genreName = genre.name, offset = 0, numberToReturn = 1),
                    timeout(Span(5, Seconds))) { artists =>

                    artists should not be empty

                    val artistGenres = artists.head.genres
                    artistGenres.size mustBe 2
                  }
                }
              }

              whenReady(genreMethods.deleteArtistRelation(ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get)),
                timeout(Span(5, Seconds))) {
                _ mustBe 1
              }
            }
          }
        }
      }
    }

    "find over genres in genre set" in {
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
      whenReady(genreMethods.save(Genre(None, "genreTest1", 'g')), timeout(Span(5, Seconds))) { genre1 =>
        whenReady(genreMethods.save(Genre(None, "genreTest2", 'r')), timeout(Span(5, Seconds))) { genre2 =>
          whenReady(genreMethods.save(Genre(None, "genreTest3", 'm')), timeout(Span(5, Seconds))) { genre3 =>
            whenReady(genreMethods.save(Genre(None, "genreTest4", 'h')), timeout(Span(5, Seconds))) { genre4 =>
              whenReady(genreMethods.save(Genre(None, "genreTest5", 'e')), timeout(Span(5, Seconds))) { genre5 =>
                whenReady(genreMethods.save(Genre(None, "genreTest6", 'j')), timeout(Span(5, Seconds))) { genre6 =>
                  whenReady(genreMethods.save(Genre(None, "genreTest7", 's')), timeout(Span(5, Seconds))) { genre7 =>
                    whenReady(genreMethods.save(Genre(None, "genreTest8", 'l')), timeout(Span(5, Seconds))) { genre8 =>
                      whenReady(genreMethods.save(Genre(None, "genreTest9", 'c')), timeout(Span(5, Seconds))) { genre9 =>
                        whenReady(genreMethods.findOverGenres(genres), timeout(Span(5, Seconds))) { _ mustBe expectedGenres }
                        whenReady(genreMethods.findOverGenres(genres), timeout(Span(5, Seconds))) { foundGenres =>
                          foundGenres ++ genres mustBe allGenres
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
