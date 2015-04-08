package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Playlist(playlistId: Option[Long], userId: String, name: String, tracks: Seq[Track])

object Playlist {
  implicit val playlistWrites = Json.writes[Playlist]

  def trackIdFormApply(trackId: Long): Long = trackId
  def trackIdFormUnapply(trackId: Long): Option[Long] = Option(trackId)

  case class PlaylistNameAndTracksId(name: String, tracksId: Seq[Long])
  def formApply(name: String, tracksId: Seq[Long]) =
    PlaylistNameAndTracksId(name, tracksId)
  def formUnapply(playlistNameAndTracksId: PlaylistNameAndTracksId) =
    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksId))

  /*def formApply(userId: Long, name: String, tracksId: Seq[Track]): Playlist =
    new Playlist(-1L, userId, name, tracksId)
  def formUnapply(playlist: Playlist): Option[(Long, String, Seq[Track])] =
    Option((playlist.userId, playlist.name, playlist.tracks))*/

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
          playlistNameAndTracksId.tracksId.foreach(trackId =>
            Track.savePlaylistTrackRelation(playlistId, trackId))
          Some(playlistId)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Cannot save playlist: " + e.getMessage)
  }

  def delete(userId: String, playlistId: Long): Boolean = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlists
          | WHERE userId = {userId}
          | AND playlistId = {playlistId} """.stripMargin)
        .on('playlistId -> playlistId)
        .execute()
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.delete: " + e.getMessage)
  }
}
