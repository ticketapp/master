package player

import java.util.UUID
import genres.Genre

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Track (uuid: UUID,
                  title: String,
                  url: String,
                  platform: Char,
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  artistName: String,
                  redirectUrl: Option[String] = None,
                  confidence: Double = 0.toDouble)

@JSExportAll
case class TrackWithGenres(track: Track, genres: Seq[Genre])