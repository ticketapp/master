package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers.{PlaylistDoesNotExistException, DAOException}
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Playlist(playlistId: Option[Long], userId: String, name: String, tracks: Seq[Track])

object Playlist {
  implicit val playlistWrites = Json.writes[Playlist]

  def idFormApply(id: Long): Long = id
  def idFormUnapply(id: Long): Option[Long] = Option(id)

  case class PlaylistNameAndTracksId(name: String, tracksId: Seq[Long])
  def formApply(name: String, tracksId: Seq[Long]) =
    PlaylistNameAndTracksId(name, tracksId)
  def formUnapply(playlistNameAndTracksId: PlaylistNameAndTracksId) =
    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksId))

  private val playlistParser: RowParser[Playlist] = {
    get[Long]("playlistId") ~
      get[String]("userId") ~
      get[String]("name") map {
      case playlistId ~ userId ~ name => Playlist.apply(Option(playlistId), userId, name, Seq.empty)
    }
  }

  def findByUserId(userId: String): Seq[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE userId = {userId}""".stripMargin)
      .on('userId -> userId)
      .as(playlistParser.*)
      .map(playlist => playlist.copy(tracks = Track.findTracksByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def save(userId: String, playlistNameAndTracksId: PlaylistNameAndTracksId): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO playlists(userId, name) VALUES({userId}, {name})""")
        .on('userId -> userId,
            'name -> playlistNameAndTracksId.name)
        .executeInsert() match {
        case None =>
          None
        case Some(playlistId: Long) =>
          playlistNameAndTracksId.tracksId.foreach(trackId => Track.savePlaylistTrackRelation(playlistId, trackId))
          Some(playlistId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save playlist: " + e.getMessage)
  }

  def delete(userId: String, playlistId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      if (existsPlaylistForUser(userId, playlistId)) {
        SQL(
          """DELETE FROM playlistsTracks
            | WHERE playlistId = {playlistId};
            |DELETE FROM playlists
            | WHERE userId = {userId}
            | AND playlistId = {playlistId}""".stripMargin)
          .on(
            'userId -> userId,
            'playlistId -> playlistId)
          .execute()
      } else {
        throw new PlaylistDoesNotExistException("There is no playlist for this user Id and this playlist Id.")
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.delete: " + e.getMessage)
  }
/*
  case class PlaylistIdAndTracksId(id: Long, tracksId: Seq[Long])
  def addOrRemoveTracksFormApply(id: Long, tracksId: Seq[Long]) =
    PlaylistIdAndTracksId(id, tracksId)
  def addOrRemoveTracksFormUnapply(playlistIdAndTracksId: PlaylistIdAndTracksId) =
    Option((playlistIdAndTracksId.id, playlistIdAndTracksId.tracksId))
*/
  case class TrackInfo(trackId: Long, action: String, trackRank: Option[Long])
  def trackInfoFormApply(trackId: Long, action: String, trackRank: Option[Long]) =
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
        for (trackInfo <- playlistIdAndTracksInfo.tracksInfo) {
          trackInfo match {
            case trackToUpdate: TrackInfo if trackInfo.action == "M" =>
              updateTrackRank(playlistIdAndTracksInfo.id, trackToUpdate)
            case trackToDelete: TrackInfo if trackInfo.action == "D" =>
              Track.deletePlaylistTrackRelation(playlistIdAndTracksInfo.id, trackToDelete.trackId)
            case trackToAdd: TrackInfo if trackInfo.action == "A" =>
              Track.savePlaylistTrackRelation(playlistIdAndTracksInfo.id, trackToAdd.trackId)
          }
        }
      } else {
        throw new PlaylistDoesNotExistException("There is no playlist for this user Id and this playlist Id.")
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.delete: " + e.getMessage)
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
