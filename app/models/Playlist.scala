package models

import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Playlist (playlistId: Long, userId: Long, name: String, tracks: Seq[Track])

object Playlist {
  implicit val playlistWrites = Json.writes[Playlist]

  
  def formApply(userId: Long, name: String, tracksId: Seq[Track]): Playlist = new Playlist(-1L, userId, name, tracksId)

  def formUnapply(playlist: Playlist): Option[(Long, String, Seq[Track])] =
    Some((playlist.userId, playlist.name, playlist.tracks))

  private val playlistParser: RowParser[Playlist] = {
    get[Long]("playlistId") ~
      get[Long]("userId") ~
      get[String]("name") map {
      case playlistId ~ userId ~ name =>
        Playlist.apply(playlistId, userId, name, Seq.empty)
    }
  }

  def findByUserId(userId: Long): Seq[Playlist] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM usersPlaylists uP
             INNER JOIN playlists p ON p.playlistId = uP.playlistId
             WHERE uP.userId = {userId}
           """)
      .on('userId -> userId)
      .as(playlistParser.*)
      .map( playlist => playlist.copy(
        tracks = Track.findTracksByPlaylistId(playlist.playlistId)
      ))
    }
  }

  def save(playlist: Playlist): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """INSERT INTO playlists(name)
            VALUES({name})
          """).on(
            'name -> playlist.name
          ).executeInsert()  match {
          case None => None
          case Some(playlistId: Long) =>
            playlist.tracks.foreach(track =>
              Track.saveTrackAndPlaylistRelation(track, playlistId)
            )
            Some(playlistId)
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save playlist: " + e.getMessage)
    }
  }
}
