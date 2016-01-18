package artistsDomain

import database.ArtistGenreRelation
import genresDomain.{GenreWithWeight, Genre}

trait artistsAndOptionalGenresToArtistsWithWeightedGenresTrait {
  def artistsAndOptionalGenresToArtistsWithWeightedGenres(seqArtistAndOptionalGenre:
                                                          Seq[(Artist, Option[(ArtistGenreRelation, Genre)])])
  : Iterable[ArtistWithWeightedGenres] = {
    val groupedByArtist = seqArtistAndOptionalGenre.groupBy(_._1)

    val artistsWithGenres = groupedByArtist map { tupleArtistSeqTupleArtistWithMaybeGenres =>
      val artist = tupleArtistSeqTupleArtistWithMaybeGenres._1
      val genresWithWeight = tupleArtistSeqTupleArtistWithMaybeGenres._2 collect {
        case (_, Some((artistGenre, genre))) => GenreWithWeight(genre, artistGenre.weight)
      }

      (artist, genresWithWeight)
    }

    artistsWithGenres map { artistWithGenre =>
      ArtistWithWeightedGenres(
        artist = artistWithGenre._1,
        genres = artistWithGenre._2.distinct.toVector)
    }
  }
}
