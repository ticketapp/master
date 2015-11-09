package models

trait TrackTransformTrait {
  def makeTrackWithGenres(trackGenreArtistTuples: Seq[(Track, Option[((Artist, ArtistGenreRelation), Genre)])]) = {
    val groupedByTracks = trackGenreArtistTuples.groupBy(_._1)

    val tracksWithGenres = groupedByTracks map { trackWithOptionalGenres =>
      val track = trackWithOptionalGenres._1
      val genreArtistRelations = trackWithOptionalGenres._2
      val genres = genreArtistRelations.collect { case (_, Some((_, genre))) =>
        genre
      }

      TrackWithGenres(track = track, genres = genres)
    }

    tracksWithGenres.toVector
  }
}