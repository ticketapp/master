import java.util.UUID

import akka.actor.Status.Success
import models._
import org.scalatest.time.{Span, Seconds}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import services.{SearchYoutubeTracks, SearchSoundCloudTracks, Utilities}
import org.scalatest.concurrent.ScalaFutures._


class TestGenreModel extends PlaySpec with OneAppPerSuite {

  "A genre" must {

    val appBuilder = new GuiceApplicationBuilder()
    val injector = appBuilder.injector()
    val dbConfProvider = injector.instanceOf[DatabaseConfigProvider]
    val utilities = new Utilities()
    val genreMethods = new GenreMethods(dbConfProvider, utilities)
    val trackMethods = new TrackMethods(dbConfProvider, utilities)
    val searchSoundCloudTracks = new SearchSoundCloudTracks(dbConfProvider, utilities, trackMethods, genreMethods)
    val searchYoutubeTracks = new SearchYoutubeTracks(dbConfProvider, genreMethods, utilities, trackMethods)
    val artistMethods = new ArtistMethods(dbConfProvider, genreMethods, searchSoundCloudTracks, searchYoutubeTracks,
      trackMethods, utilities)

    "be saved and deleted" in {
      val genre = Genre(None, "rockadocka", 'r')
      whenReady(genreMethods.save(genre), timeout(Span(5, Seconds))) { genreSaved =>
        whenReady(genreMethods.findById(genreSaved.id.get), timeout(Span(5, Seconds))) { genreFound =>

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
      whenReady(genreMethods.saveOrFind(genre), timeout(Span(5, Seconds))) { genreFound =>
        whenReady(genreMethods.saveOrFind(genre), timeout(Span(5, Seconds))) { secondGenreFound =>
          try {
            genreFound mustBe secondGenreFound
          } finally {
            whenReady(genreMethods.delete(genreFound.id.get), timeout(Span(5, Seconds))) { _ mustBe 1 }
            whenReady(genreMethods.delete(secondGenreFound.id.get), timeout(Span(5, Seconds))) { _ mustBe 0 }
          }
        }
      }
    }

    "save and delete its relation with an artist" in {
      val genre = Genre(None, "rockiyadockia")
      val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
        "artistFacebookUrlTestGenre", Set("website"))
      whenReady(genreMethods.saveOrFind(genre), timeout(Span(5, Seconds))) { savedGenre =>
        whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
          try {
            whenReady(genreMethods.saveArtistRelation(genreMethods.ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { artistGenreRelation =>

              artistGenreRelation mustBe true

              whenReady(artistMethods.findAllByGenre("rockiyadockia", 1, 0), timeout(Span(5, Seconds))) { artistsSet =>

                assert(artistsSet.nonEmpty)

                whenReady(genreMethods.deleteArtistRelation(genreMethods.ArtistGenreRelation(savedArtist.id.get, savedGenre.id.get)),
                timeout(Span(5, Seconds))) { _ mustBe Success(1) }
              }
            }
          } finally {
            genreMethods.delete(savedGenre.id.get)
            artistMethods.delete(savedArtist.id.get)
          }
        }
      }
    }

    "save and delete its relation with a track" in {
     val genre = Genre(None, "rockerdocker")
     val artist = Artist(None, Option("facebookId"), "artistTest", Option("imagePath"), Option("description"),
       "artistFacebookUrlTestGenre2", Set("website"))
     val trackId = UUID.randomUUID
     val track = Track(trackId, "titleTestGenreModel1", "url", 's', "thumbnailUrl", "artistFacebookUrlTestGenre2", "artistName")
     whenReady(genreMethods.saveOrFind(genre), timeout(Span(5, Seconds))) { savedGenre =>
       whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
         whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
           try {
             whenReady(genreMethods.saveTrackRelation(genreMethods.TrackGenreRelation(trackId, savedGenre.id.get)),
               timeout(Span(5, Seconds))) { trackGenreRelation =>

               trackGenreRelation mustBe Success(true)

               whenReady(trackMethods.findByGenre("rockerdocker", 1, 0), timeout(Span(5, Seconds))) { tracksSet =>

                 assert(tracksSet.nonEmpty)

                 whenReady(genreMethods.deleteTrackRelation(genreMethods.TrackGenreRelation(trackId, savedGenre.id.get)),
                   timeout(Span(5, Seconds))) { _ mustBe Success(1) }
               }
             }
           } finally {
             trackMethods.delete(trackId)
             artistMethods.delete(savedArtist.id.get)
             genreMethods.delete(savedGenre.id.get)
           }
         }
       }
     }
    }

    "find all genres for a track" in {
      val artist = Artist(None, Option("facebookIdTestGenreModel"), "artistTest", Option("imagePath"),
        Option("description"), "artistFacebookUrlTestGenreModel", Set("website"))
      val trackId = UUID.randomUUID
      val track = Track(trackId, "titleTestUserModel", "url2", 's', "thumbnailUrl",
        "artistFacebookUrlTestGenreModel", "artistName")
      val genre = Genre(None, "pilipilipims")
      whenReady(artistMethods.save(artist), timeout(Span(5, Seconds))) { savedArtist =>
        whenReady(genreMethods.saveOrFind(genre), timeout(Span(5, Seconds))) { savedGenre =>
          try {
            whenReady(trackMethods.save(track), timeout(Span(5, Seconds))) { savedTrack =>
              whenReady(genreMethods.saveTrackRelation(genreMethods.TrackGenreRelation(trackId, savedGenre.id.get)),
              timeout(Span(5, Seconds))) { genreTrackRelation =>
                whenReady(genreMethods.findAllByTrack(trackId), timeout(Span(5, Seconds))) { tracksSeq =>

                  tracksSeq mustBe Seq(genre.copy(id = Some(savedGenre.id.get)))

                  whenReady(genreMethods.deleteTrackRelation(genreMethods.TrackGenreRelation(trackId, savedGenre.id.get)),
                    timeout(Span(5, Seconds))) { _ mustBe Success(1) }
                }
              }
            }
          } finally {
              genreMethods.delete(savedGenre.id.get)
              trackMethods.delete(trackId)
              artistMethods.delete(savedArtist.id.get)
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
      whenReady(genreMethods.saveOrFind(Genre(None, "genreTest1", 'g')), timeout(Span(5, Seconds))) { genre1 =>
        whenReady(genreMethods.saveOrFind(Genre(None, "genreTest2", 'r')), timeout(Span(5, Seconds))) { genre2 =>
          whenReady(genreMethods.saveOrFind(Genre(None, "genreTest3", 'm')), timeout(Span(5, Seconds))) { genre3 =>
            whenReady(genreMethods.saveOrFind(Genre(None, "genreTest4", 'h')), timeout(Span(5, Seconds))) { genre4 =>
              whenReady(genreMethods.saveOrFind(Genre(None, "genreTest5", 'e')), timeout(Span(5, Seconds))) { genre5 =>
                whenReady(genreMethods.saveOrFind(Genre(None, "genreTest6", 'j')), timeout(Span(5, Seconds))) { genre6 =>
                  whenReady(genreMethods.saveOrFind(Genre(None, "genreTest7", 's')), timeout(Span(5, Seconds))) { genre7 =>
                    whenReady(genreMethods.saveOrFind(Genre(None, "genreTest8", 'l')), timeout(Span(5, Seconds))) { genre8 =>
                      whenReady(genreMethods.saveOrFind(Genre(None, "genreTest9", 'c')), timeout(Span(5, Seconds))) { genre9 =>
                        try {
                          whenReady(genreMethods.findOverGenres(genres), timeout(Span(5, Seconds))) { _ mustBe expectedGenres }
                          whenReady(genreMethods.findOverGenres(genres), timeout(Span(5, Seconds))) { foundGenres =>
                            foundGenres ++ genres mustBe allGenres
                          }
                        } finally {
                          genreMethods.delete(genre1.id.get)
                          genreMethods.delete(genre2.id.get)
                          genreMethods.delete(genre3.id.get)
                          genreMethods.delete(genre4.id.get)
                          genreMethods.delete(genre5.id.get)
                          genreMethods.delete(genre6.id.get)
                          genreMethods.delete(genre7.id.get)
                          genreMethods.delete(genre8.id.get)
                          genreMethods.delete(genre9.id.get)
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
