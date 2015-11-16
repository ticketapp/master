package models

import scala.collection.immutable.Seq


trait artistsAndOptionalGenresToArtistsWithWeightedGenresAndHasTrack {
  def artistsAndOptionalGenresToArtistsWithWeightedGenresAndHasTrack(seqArtistAndOptionalGenre:
                                                                     scala.Seq[((Artist, Option[Track]), Option[(ArtistGenreRelation, Genre)])])
  : Iterable[ArtistWithWeightedGenresAndHasTrack] = {
    val groupedByArtist = seqArtistAndOptionalGenre.groupBy(_._1._1)

    val artistsWithGenresAndHasTracks = groupedByArtist map { tupleArtistSeqTupleArtistWithMaybeGenresAndMaybeTrack =>
      val artist = tupleArtistSeqTupleArtistWithMaybeGenresAndMaybeTrack._1
      val genresWithWeight = tupleArtistSeqTupleArtistWithMaybeGenresAndMaybeTrack._2 collect {
        case (_, Some((artistGenre, genre))) => GenreWithWeight(genre, artistGenre.weight)
      }

      val hasTracks = tupleArtistSeqTupleArtistWithMaybeGenresAndMaybeTrack._2 exists {
        case ((_, Some(_)), _) => true
        case _ => false
      }
      (artist, genresWithWeight, hasTracks)
    }

    artistsWithGenresAndHasTracks map { artistWithGenreAndHasTrack =>
      ArtistWithWeightedGenresAndHasTrack(
        artist = artistWithGenreAndHasTrack._1,
        genres = artistWithGenreAndHasTrack._2.to[Seq].distinct,
        hasTracks = artistWithGenreAndHasTrack._3)
    }
  }
}
