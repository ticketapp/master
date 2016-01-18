
package playlistsDomain

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._

trait playlistFormsTrait extends {

  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "trackIds" -> seq(mapping(
      "trackId" -> nonEmptyText(6),
      "trackRank" -> longNumber
    )(trackIdAndRankFormApply)(trackIdAndRankFormUnapply))
  )(playlistFormApply)(playlistFormUnapply))

  def trackIdAndRankFormApply(stringUUID: String, rank: Long) = TrackIdWithPlaylistRank(UUID.fromString(stringUUID), rank.toDouble)

  def trackIdAndRankFormUnapply(trackIdAndRank: TrackIdWithPlaylistRank) = Option((trackIdAndRank.trackId.toString,
    trackIdAndRank.rank.toLong))

  def playlistFormApply(name: String, tracksIdAndRank: Seq[TrackIdWithPlaylistRank]) =
      PlaylistNameTracksIdAndRank(name, tracksIdAndRank.toVector)

  def playlistFormUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
      Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))
}


//  def idAndRankFormApply(stringUUID: String, rank: BigDecimal) = TrackUUIDAndRank(UUID.fromString(stringUUID), rank)
//  def idAndRankFormUnapply(trackIdAndRank: TrackUUIDAndRank) = Option((trackIdAndRank.UUID.toString, trackIdAndRank.rank))
//
//  case class PlaylistNameTracksIdAndRank(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank])
//  def formApply(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank]) =
//    PlaylistNameTracksIdAndRank(name, tracksIdAndRank)
//  def formUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
//    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))