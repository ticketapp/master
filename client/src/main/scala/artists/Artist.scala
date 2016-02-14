package artists

import genres.GenreWithWeight

import scala.collection.immutable.Seq
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Artist(id: Option[Long] = None,
                  facebookId: Option[String] = None,
                  name: String,
                  imagePath: Option[String] = None,
                  description: Option[String] = None,
                  facebookUrl: String,
                  websites: Set[String],
                  hasTracks: Boolean,
                  likes: Option[Int] = None,
                  country: Option[String] = None)

@JSExportAll
case class ArtistWithWeightedGenres(artist: Artist, genres: Seq[GenreWithWeight])
