package models

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import controllers.{PlaylistUpdateTrackWithoutRankException, PlaylistDoesNotExistException, DAOException}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import services.MyPostgresDriver.api._

import play.api.libs.json.Json
import play.api.Play.current
import json.JsonHelper._
import play.api.Logger
import services._

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}


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

  def save(playlistInfo: Playlist): Future[Playlist] =
    db.run(playlists returning playlists.map(_.id) into ((playlist, id) => playlist.copy(playlistId = Option(id))) += playlistInfo)

  def find(id: Long): Future[Option[Playlist]] =  db.run(playlists.filter(_.id === id).result.headOption)

  def delete(id: Long): Future[Int] = db.run(playlists.filter(_.id === id).delete)

  def saveTrackRelation(playlistTrack: PlaylistTrack): Future[Int] = db.run(playlistsTracks += playlistTrack)

//  def saveWithTrackRelation(userId: UUID, playlistNameTracksIdAndRank: PlaylistNameTracksIdAndRank): Long = {
//    save(Playlist(None, userId, playlistNameTracksIdAndRank.name, Seq.empty)) match {
//      case Success(Some(playlistId: Long)) =>
//        playlistNameTracksIdAndRank.tracksIdAndRank.foreach(trackIdAndRank =>
//          saveTrackRelation(playlistId, trackIdAndRank))
//        playlistId
//      case _ =>
//        throw new DAOException("Playlist.saveWithTrackRelation")
//    }
//  }
/*
  def findByUserId(userUUID: UUID): Seq[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE userId = {userId}""".stripMargin)
      .on('userId -> userUUID)
      .as(playlistParser.*)
      .map(playlist => playlist.copy(tracks = Track.findByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def deleteTracksRelations(userId: UUID, playlistId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          |  WHERE playlistId = {playlistId}""".stripMargin)
        .on(
          'userId -> userId,
          'playlistId -> playlistId)
        .executeUpdate()
    }
  }


  def deleteTrackRelation(playlistId: Long, trackId: UUID): Int = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          | WHERE playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("deleteTrackRelation: " + e.getMessage)
  }

  case class TrackInfo(trackId: UUID, action: String, trackRank: Option[BigDecimal])
  def trackInfoFormApply(trackId: String, action: String, trackRank: Option[BigDecimal]) =
    TrackInfo(UUID.fromString(trackId), action, trackRank)
  def trackInfoFormUnapply(trackInfo: TrackInfo) =
    Option((trackInfo.trackId.toString, trackInfo.action, trackInfo.trackRank))

  case class PlaylistIdAndTracksInfo(id: Long, tracksInfo: Seq[TrackInfo])
  def updateFormApply(id: Long, tracksInfo: Seq[TrackInfo]) =
    PlaylistIdAndTracksInfo(id, tracksInfo)
  def updateFormUnapply(playlistIdAndTracksInfo: PlaylistIdAndTracksInfo) =
    Option((playlistIdAndTracksInfo.id, playlistIdAndTracksInfo.tracksInfo))

  def existsPlaylistForUser(userId: UUID, playlistId: Long)(implicit connection: Connection): Boolean = try {
    SQL(
      """SELECT exists(SELECT 1 FROM playlists
        |  WHERE userId = {userId} AND playlistId = {playlistId})""".stripMargin)
      .on(
        "userId" -> userId,
        "playlistId" -> playlistId)
      .as(scalar[Boolean].single)
  } catch {
    case e: Exception => throw new DAOException("Artist.isArtistFollowed: " + e.getMessage)
  }

  def update(userId: UUID, playlistIdAndTracksInfo: PlaylistIdAndTracksInfo): Unit = try {
    DB.withConnection { implicit connection =>
      if (existsPlaylistForUser(userId, playlistIdAndTracksInfo.id)) {
        for (trackInfo <- playlistIdAndTracksInfo.tracksInfo)
          proceedTrackUpdateDependingOfAction(playlistIdAndTracksInfo.id, trackInfo)
      } else {
        throw new PlaylistDoesNotExistException("There is no playlist for this user Id and this playlist Id.")
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.delete: " + e.getMessage)
  }

  def proceedTrackUpdateDependingOfAction(playlistId: Long, trackInfo: TrackInfo): Unit = trackInfo match {
    case trackToUpdate: TrackInfo if trackInfo.action == "M" =>
      updateTrackRank(playlistId, trackToUpdate)
    case trackToDelete: TrackInfo if trackInfo.action == "D" =>
      deleteTrackRelation(playlistId, trackToDelete.trackId)
    case trackToAdd: TrackInfo if trackInfo.action == "A" =>
      try {
        saveTrackRelation(playlistId, TrackUUIDAndRank(trackToAdd.trackId, trackToAdd.trackRank.get))
      } catch {
        case e: NoSuchElementException => PlaylistUpdateTrackWithoutRankException("Playlist.update")
      }
  }

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

  def addTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.savePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }

  def deleteTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.deletePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }*/
}
