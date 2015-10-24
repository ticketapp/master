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


case class PlaylistWithTracks(playlistInfo: Playlist, tracksWithRank: Seq[TrackWithPlaylistRank])

case class Playlist(playlistId: Option[Long], userId: UUID, name: String)

case class TrackWithPlaylistRank(track: Track, rank: Double)

class PlaylistMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
//                         val genreMethods: GenreMethods,
//                         val searchSoundCloudTracks: SearchSoundCloudTracks,
//                         val searchYoutubeTracks: SearchYoutubeTracks,
//                         val trackMethods: TrackMethods,
                         val utilities: Utilities)
  extends HasDatabaseConfigProvider[MyPostgresDriver] with SoundCloudHelper with MyDBTableDefinitions {
//

//  def idAndRankFormApply(stringUUID: String, rank: BigDecimal) = TrackUUIDAndRank(UUID.fromString(stringUUID), rank)
//  def idAndRankFormUnapply(trackIdAndRank: TrackUUIDAndRank) = Option((trackIdAndRank.UUID.toString, trackIdAndRank.rank))
//
//  case class PlaylistNameTracksIdAndRank(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank])
//  def formApply(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank]) =
//    PlaylistNameTracksIdAndRank(name, tracksIdAndRank)
//  def formUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
//    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))

  def save(playlist: Playlist): Future[Playlist] =
    db.run(playlists returning playlists.map(_.id) into ((playlist, id) => playlist.copy(playlistId = Option(id))) += playlist)

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
          val seqTracksWithRating = iterableTracksWithRating.toVector
          Option(PlaylistWithTracks(playlist, seqTracksWithRating))
      }
    }
  }

  def delete(id: Long): Future[Int] = db.run(playlists.filter(_.id === id).delete)

  def saveTrackRelation(playlistTrack: PlaylistTrack): Future[Int] = db.run(playlistsTracks += playlistTrack)

  def saveTracksRelation(playlistTracks: Seq[PlaylistTrack]): Any = {
    val a = playlistsTracks ++= playlistTracks
    db.run(a) recover {
      case e: Exception => Logger.error("Playlist.saveTracksRelation: ", e)
    }
  }

  def saveWithTracks(playlist: Playlist, tracksWithPlaylistRank: Seq[TrackWithPlaylistRank]): Future[Long] = {
    val query = for {
      playlistId <- playlists returning playlists.map(_.id) += playlist
      track <- tracks ++= tracksWithPlaylistRank.map(_.track)
      playlistTrackRelation <- playlistsTracks ++= tracksWithPlaylistRank.map(trackWithPlaylistRank =>
        PlaylistTrack(playlistId, trackWithPlaylistRank.track.uuid, trackWithPlaylistRank.rank))
    } yield { playlistId }

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
      playlistsWithTracks map(playlistWithTrack => PlaylistWithTracks(playlistWithTrack._1, playlistWithTrack._2.to[Seq]))
    } map { _.toVector }
  }
//
//    def deleteTracksRelations(userId: UUID, playlistId: Long): Try[Int] = Try {
//      DB.withConnection { implicit connection =>
//        SQL(
//          """DELETE FROM playlistsTracks
//            |  WHERE playlistId = {playlistId}""".stripMargin)
//          .on(
//            'userId -> userId,
//            'playlistId -> playlistId)
//          .executeUpdate()
//      }
//    }


//    def deleteTrackRelation(playlistId: Long, trackId: UUID): Int = try {
//      DB.withConnection { implicit connection =>
//        SQL(
//          """DELETE FROM playlistsTracks
//            | WHERE playlistId = {playlistId}""".stripMargin)
//          .on('playlistId -> playlistId)
//          .executeUpdate()
//      }
//    } catch {
//      case e: Exception => throw new DAOException("deleteTrackRelation: " + e.getMessage)
//    }

//    case class TrackInfo(trackId: UUID, action: String, trackRank: Option[BigDecimal])
//    def trackInfoFormApply(trackId: String, action: String, trackRank: Option[BigDecimal]) =
//      TrackInfo(UUID.fromString(trackId), action, trackRank)
//    def trackInfoFormUnapply(trackInfo: TrackInfo) =
//      Option((trackInfo.trackId.toString, trackInfo.action, trackInfo.trackRank))
//
//    case class PlaylistIdAndTracksInfo(id: Long, tracksInfo: Seq[TrackInfo])
//    def updateFormApply(id: Long, tracksInfo: Seq[TrackInfo]) =
//      PlaylistIdAndTracksInfo(id, tracksInfo)
//    def updateFormUnapply(playlistIdAndTracksInfo: PlaylistIdAndTracksInfo) =
//      Option((playlistIdAndTracksInfo.id, playlistIdAndTracksInfo.tracksInfo))

//    def existsPlaylistForUser(userId: UUID, playlistId: Long): Boolean = {
//      SQL(
//        """SELECT exists(SELECT 1 FROM playlists
//          |  WHERE userId = {userId} AND playlistId = {playlistId})""".stripMargin)
//        .on(
//          "userId" -> userId,
//          "playlistId" -> playlistId)
//        .as(scalar[Boolean].single)
//    }


/*
      def updateTrackRank(playlistId: Long, trackInfo: TrackInfo): Unit = try {
        DB.withConnection { implicit connection =>
          SQL(
            """UPDATE playlistsTracks
              | SET rank = {trackRank}
              | WHERE playlistId = {playlistId} AND trackId = {trackId}""".stripMargin)
            .on(
              'playlistId -> playlistId,
              'trackId -> trackInfo.trackId,
              'trackRank -> trackInfo.trackRank)
            .executeUpdate()
        }
      } catch {
        case e: Exception => throw new DAOException("Playlist.updateTrackRank: " + e.getMessage)
      }

    def addTracksInPlaylist(playlist: Playlist, tracksWithPlaylistRank: Seq[TrackWithPlaylistRank]): Unit = {
      val query = for {
        playlistId <- playlists returning playlists.map(_.id) += playlist
        track <- tracks ++= tracksWithPlaylistRank.map(_.track)
        playlistTrackRelation <- playlistsTracks ++= tracksWithPlaylistRank.map(trackWithPlaylistRank =>
          PlaylistTrack(playlistId, trackWithPlaylistRank.track.uuid, trackWithPlaylistRank.rank))
      } yield { playlistId }

      db.run(query)
    }

    def deleteTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
      playlistIdAndTracksId.tracksId.foreach(trackId =>
        Track.deletePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
    }*/
}
