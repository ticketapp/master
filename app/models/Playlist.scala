package models

import java.util.UUID
import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.MyPostgresDriver.api._
import services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Playlist(playlistId: Option[Long], userId: UUID, name: String)

case class PlaylistWithTracks(playlistInfo: Playlist, tracksWithRank: Vector[TrackWithPlaylistRank])

case class PlaylistWithTracksWithGenres(playlistInfo: Playlist, tracksWithRankAndGenres: Vector[TrackWithPlaylistRankAndGenres])

case class PlaylistWithTracksIdAndRank(playlistInfo: Playlist, tracksWithRank: Vector[TrackIdWithPlaylistRank])

case class PlaylistNameTracksIdAndRank(name: String, tracksIdAndRank: Vector[TrackIdWithPlaylistRank])

case class TrackWithPlaylistRank(track: Track, rank: Double)

case class TrackWithPlaylistRankAndGenres(track: TrackWithGenres, rank: Double)

case class TrackIdWithPlaylistRank(trackId: UUID, rank: Double)


class PlaylistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with SoundCloudHelper
    with MyDBTableDefinitions
    with TrackTransformTrait {

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

  def update(playlist: PlaylistWithTracksIdAndRank): Future[Long] = playlist.playlistInfo.playlistId match {
    case Some(playlistId) =>
      val query = (for {
        result <- playlists.filter(_.id === playlistId).delete
        if result == 1
        playlistId <- playlists returning playlists.map(_.id) += playlist.playlistInfo
        playlistTrackRelation <- playlistsTracks ++= playlist.tracksWithRank.map(trackWithPlaylistRank =>
          PlaylistTrack(playlistId, trackWithPlaylistRank.trackId, trackWithPlaylistRank.rank))
      } yield playlistId).transactionally
      db.run(query)
    case None =>
      Future(0)
  }

  def findByUserId(userUUID: UUID): Future[Vector[PlaylistWithTracksWithGenres]] = {
    val query = for {
      (playlist, optionalPlaylistTrackAndTrack) <- playlists joinLeft
        ((playlistsTracks join tracks on (_.trackId === _.uuid)) joinLeft
          (artists join artistsGenres on (_.id === _.artistId) join genres on (_._2.genreId === _.id)) on
        (_._2.artistFacebookUrl === _._1._1.facebookUrl)) on (_.id === _._1._1.playlistId)
    } yield (playlist, optionalPlaylistTrackAndTrack)

    db.run(query.filter(_._1.userId === userUUID).result) map { seqPlaylistAndOptionalTrackRank =>

      val groupedByPlaylist = seqPlaylistAndOptionalTrackRank.groupBy(_._1)

      groupedByPlaylist map { playlistWithTracksWithRelations =>
        val playlist = playlistWithTracksWithRelations._1
        val playlistWithTrackWithRelation = playlistWithTracksWithRelations._2

        val optionalTracksWithRelation = playlistWithTrackWithRelation.map(_._2)

        val tracksWithRelation = optionalTracksWithRelation collect {
          case Some(trackWithOptionalRelation) => trackWithOptionalRelation
        }

        PlaylistWithTracksWithGenres(
          playlistInfo = playlist,
          tracksWithRankAndGenres = makeTrackWithPlaylistRankAndGenres(tracksWithRelation))
      }

    } map { _.toVector }
  }
}
