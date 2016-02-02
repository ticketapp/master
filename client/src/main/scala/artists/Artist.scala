package artists

import genres.GenreWithWeight

import scala.collection.immutable.Seq
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Artist(id: Option[Long],
                  facebookId: Option[String],
                  name: String,
                  imagePath: Option[String],
                  description: Option[String],
                  facebookUrl: String,
                  websites: Set[String],
                  hasTracks: Boolean,
                  likes: Option[Int],
                  country: Option[String])

@JSExportAll
case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight])
