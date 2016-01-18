package tracksDomain

import artistsDomain.Artist
import database.{PlaylistTrack, ArtistGenreRelation}
import genresDomain.Genre
import playlistsDomain.TrackWithPlaylistRankAndGenres

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

  def makeTrackWithPlaylistRankAndGenres(tracksWithRelation: Seq[((PlaylistTrack, Track),
    Option[((Artist, ArtistGenreRelation), Genre)])]): Vector[TrackWithPlaylistRankAndGenres] = {
    val groupedByTracks = tracksWithRelation.groupBy(_._1)

    val trackWithPlaylistRankAndGenres = groupedByTracks map { trackWithOptionalGenres =>
      val track = trackWithOptionalGenres._1
      val genreArtistRelations = trackWithOptionalGenres._2
      val genres = genreArtistRelations.collect { case (_, Some((_, genre))) =>
        genre
      }

      TrackWithPlaylistRankAndGenres(TrackWithGenres(track = track._2, genres = genres), track._1.trackRank)
    }

    trackWithPlaylistRankAndGenres.toVector
  }
}