package models

import java.util.UUID
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.MyPostgresDriver.api._
import services._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Playlist(playlistId: Option[Long], userId: UUID, name: String)

case class PlaylistWithTracks(playlistInfo: Playlist, tracksWithRank: Vector[TrackWithPlaylistRank])

case class PlaylistWithTracksIdAndRank(playlistInfo: Playlist, tracksWithRank: Vector[TrackIdWithPlaylistRank])

case class TrackWithPlaylistRank(track: Track, rank: Double)

case class TrackIdWithPlaylistRank(trackId: UUID, rank: Double)


class PlaylistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                val utilities: Utilities)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with SoundCloudHelper with MyDBTableDefinitions {

  def find(id: Long): Future[Option[PlaylistWithTracks]] = {
    val query = for {
      (playlist, optionalPlaylistTrackAndTrack) <- playlists joinLeft
        (playlistsTracks join tracks on (_.trackId === _.uuid)) on (_.id === _._1.playlistId)
    } yield (playlist, optionalPlaylistTrackAndTrack)

    db.run(query.filter(_._1.id === id).result) map { seqPlaylistAndOptionalTrackRank =>
      val groupedByPlaylist = seqPlaylistAndOptionalTrackRank.groupBy(_._1)
      val maybePlaylist = groupedByPlaylist.keys.headOption
      maybePlaylist match {
        case None =>
          None
        case Some(playlist) =>
          val iterableTracksWithRating = groupedByPlaylist flatMap {
            _._2 collect {
              case (_, Some(playlistTrackWithTrack)) =>
                TrackWithPlaylistRank(playlistTrackWithTrack._2, playlistTrackWithTrack._1.trackRank)
            }
          }
          val seqTracksWithRating = iterableTracksWithRating.toVector.sortBy(_.rank)
          Option(PlaylistWithTracks(playlist, seqTracksWithRating))
      }
    }
  }

  def delete(id: Long): Future[Int] = db.run(playlists.filter(_.id === id).delete)

  def saveWithTrackRelations(playlistWithTracksIdAndRank: PlaylistWithTracksIdAndRank): Future[Long] = {
    val query = for {
      playlistId <- playlists returning playlists.map(_.id) += playlistWithTracksIdAndRank.playlistInfo
      playlistTrackRelation <- playlistsTracks ++= playlistWithTracksIdAndRank.tracksWithRank.map(trackWithPlaylistRank =>
        PlaylistTrack(playlistId, trackWithPlaylistRank.trackId, trackWithPlaylistRank.rank))
    } yield playlistId
    
    db.run(query)
  }

  def findByUserId(userUUID: UUID): Future[Vector[PlaylistWithTracks]] = {
    val query = for {
      (playlist, optionalPlaylistTrackAndTrack) <- playlists joinLeft
        (playlistsTracks join tracks on (_.trackId === _.uuid)) on (_.id === _._1.playlistId)
    } yield (playlist, optionalPlaylistTrackAndTrack)

    db.run(query.filter(_._1.userId === userUUID).result) map { seqPlaylistAndOptionalTrackRank =>
      val groupedByPlaylist = seqPlaylistAndOptionalTrackRank.groupBy(_._1)

      val playlistsWithTracks = groupedByPlaylist map { tuplePlaylistSeqTuplePlaylistWithMaybeTracks =>
        (tuplePlaylistSeqTuplePlaylistWithMaybeTracks._1, tuplePlaylistSeqTuplePlaylistWithMaybeTracks._2 collect {
          case (_, Some((playlistTrack, track))) => TrackWithPlaylistRank(track, playlistTrack.trackRank)
        })
      }
      playlistsWithTracks map(playlistWithTrack => PlaylistWithTracks(playlistWithTrack._1,
        playlistWithTrack._2.to[Vector].sortBy(_.rank)))
    } map { _.toVector }
  }
}
