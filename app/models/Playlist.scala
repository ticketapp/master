package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.{PlaylistUpdateTrackWithoutRankException, PlaylistDoesNotExistException, DAOException}
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import json.JsonHelper._

import scala.util.{Try, Success, Failure}

case class Playlist(playlistId: Option[Long], userId: String, name: String, tracks: Seq[Track])

object Playlist {

  case class TrackIdAndRank(id: String, rank: BigDecimal)
  def idAndRankFormApply(id: String, rank: BigDecimal) = TrackIdAndRank(id, rank)
  def idAndRankFormUnapply(trackIdAndRank: TrackIdAndRank) = Option((trackIdAndRank.id, trackIdAndRank.rank))

  case class PlaylistNameTracksIdAndRank(name: String, tracksIdAndRank: Seq[TrackIdAndRank])
  def formApply(name: String, tracksIdAndRank: Seq[TrackIdAndRank]) =
    PlaylistNameTracksIdAndRank(name, tracksIdAndRank)
  def formUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))

  private val playlistParser: RowParser[Playlist] = {
    get[Long]("playlistId") ~
      get[String]("userId") ~
      get[String]("name") map {
      case playlistId ~ userId ~ name => Playlist(Option(playlistId), userId, name, Seq.empty)
    }
  }

  def find(playlistId: Long): Seq[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .as(playlistParser.*)
        .map(playlist => playlist.copy(tracks = Track.findByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def findByUserId(userId: String): Seq[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE userId = {userId}""".stripMargin)
      .on('userId -> userId)
      .as(playlistParser.*)
      .map(playlist => playlist.copy(tracks = Track.findByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def save(playlist: Playlist): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO playlists(userId, name) VALUES({userId}, {name})""")
        .on(
          'userId -> playlist.userId,
          'name -> playlist.name)
        .executeInsert()
    }
  }

  def saveWithTrackRelation(userId: String, playlistNameTracksIdAndRank: PlaylistNameTracksIdAndRank): Long = {
    save(Playlist(None, userId, playlistNameTracksIdAndRank.name, Seq.empty)) match {
      case Success(Some(playlistId: Long)) =>
        playlistNameTracksIdAndRank.tracksIdAndRank.foreach(trackIdAndRank =>
          savePlaylistTrackRelation(playlistId, trackIdAndRank))
        playlistId
      case _ =>
        throw new DAOException("Playlist.saveWithTrackRelation")
      }
  }

  def savePlaylistTrackRelation(playlistId: Long, trackIdAndRank: TrackIdAndRank): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO playlistsTracks (playlistId, trackId, trackRank)
          |  VALUES ({playlistId}, {trackId}, {trackRank})""".stripMargin)
        .on(
          'playlistId -> playlistId,
          'trackId -> trackIdAndRank.id,
          'trackRank -> trackIdAndRank.rank.bigDecimal)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Track.savePlaylistTrackRelation: " + e.getMessage)
  }

  def delete(userId: String, playlistId: Long): Try[Boolean] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          |  WHERE playlistId = {playlistId};
          |DELETE FROM playlists
          |  WHERE userId = {userId}
          |  AND playlistId = {playlistId}""".stripMargin)
        .on(
          'userId -> userId,
          'playlistId -> playlistId)
        .execute()
    }
  }

  case class TrackInfo(trackId: String, action: String, trackRank: Option[BigDecimal])
  def trackInfoFormApply(trackId: String, action: String, trackRank: Option[BigDecimal]) =
    TrackInfo(trackId, action, trackRank)
  def trackInfoFormUnapply(trackInfo: TrackInfo) =
    Option((trackInfo.trackId, trackInfo.action, trackInfo.trackRank))

  case class PlaylistIdAndTracksInfo(id: Long, tracksInfo: Seq[TrackInfo])
  def updateFormApply(id: Long, tracksInfo: Seq[TrackInfo]) =
    PlaylistIdAndTracksInfo(id, tracksInfo)
  def updateFormUnapply(playlistIdAndTracksInfo: PlaylistIdAndTracksInfo) =
    Option((playlistIdAndTracksInfo.id, playlistIdAndTracksInfo.tracksInfo))

  def existsPlaylistForUser(userId: String, playlistId: Long)(implicit connection: Connection): Boolean = try {
    SQL(
      """SELECT exists(SELECT 1 FROM playlists
        |  WHERE userId = {userId} AND playlistId = {playlistId})""".stripMargin)
      .on("userId" -> userId,
        "playlistId" -> playlistId)
      .as(scalar[Boolean].single)
  } catch {
    case e: Exception => throw new DAOException("Artist.isArtistFollowed: " + e.getMessage)
  }

  def update(userId: String, playlistIdAndTracksInfo: PlaylistIdAndTracksInfo): Unit = try {
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
      Track.deletePlaylistTrackRelation(playlistId, trackToDelete.trackId)
    case trackToAdd: TrackInfo if trackInfo.action == "A" =>
      try {
        savePlaylistTrackRelation(playlistId, TrackIdAndRank(trackToAdd.trackId, trackToAdd.trackRank.get))
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
/*
  def addTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.savePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }

  def deleteTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.deletePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }*/
}
