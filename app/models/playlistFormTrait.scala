/*
package models

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._

trait playlistFormTrait extends {
  
  val playlistBindingForm = Form(mapping(
    "name" -> nonEmptyText,
    "tracksId" -> seq(mapping(
      "trackId" -> nonEmptyText(6),
      "trackRank" -> number
    )(playlistIdAndRankFormApply)(playlistIdAndRankFormUnapply))
  )(playlistFormApply)(playlistFormUnapply))


  def playlistIdAndRankFormApply(stringUUID: String, rank: Double) = TrackUUIDAndRank(UUID.fromString(stringUUID), rank.toDouble)

  def playlistIdAndRankFormUnapply(trackIdAndRank: TrackUUIDAndRank) = Option((trackIdAndRank.trackUUID.toString,
    trackIdAndRank.rank.toString))

  def playlistFormApply(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank]) =
      PlaylistNameTracksIdAndRank(name, tracksIdAndRank)

  def playlistFormUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
      Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))
}
*/
