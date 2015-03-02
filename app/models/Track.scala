package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Track (trackId: Long, name: String, url: String, platform: String)

object Track {
  implicit val trackWrites = Json.writes[Track]

  def formApply(name: String, url: String, platform: String) = new Track(-1L, name, url, platform)

  def formUnapply(track: Track) = Some(track.name, track.url ,track.platform)

  private val TrackParser: RowParser[Track] = {
    get[Long]("trackId") ~
      get[String]("name") ~
      get[String]("url") ~
      get[String]("platform") map {
      case trackId ~ name ~ url ~ platform =>
        Track(trackId, name, url, platform)
    }
  }

  def findAll(): Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("select * from tracks")
        .as(TrackParser.*)
    }
  }

  def findAllByArtist(artistId: Long): Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM artistsTracks aT
             INNER JOIN tracks t ON t.trackId = aT.trackId where aT.artistId = {artistId}""")
        .on('artistId -> artistId)
        .as(TrackParser.*)
    }
  }

  def findTracksByPlaylistId(playlistId: Long): Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM playlistsTracks pT
             INNER JOIN tracks t ON t.trackId = pT.trackId where pT.eventId = {eventId}""")
        .on('playlistId -> playlistId)
        .as(TrackParser.*)
    }
  }

  def find(trackId: Long): Option[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from tracks WHERE trackId = {trackId}")
        .on('trackId -> trackId)
        .as(TrackParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Track] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM tracks WHERE LOWER(name) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(TrackParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Track.findAllContaining: " + e.getMessage)
    }
  }


  def saveTrackAndPlaylistRelation(track: Track, id: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( s"""INSERT into tracks(name)
        values ({name})"""
        ).on(
            'name -> track.name
          ).executeInsert() match {
          case Some(x: Long) => savePlaylistTrackRelation(id, x)
          case _ => None
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create track: " + e.getMessage)
    }
  }

  def savePlaylistTrackRelation(playlistId: Long, trackId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO playlistsTracks (playlistId, trackId)
            VALUES ({playlistId}, {trackId})""").on(
            'playlistId -> playlistId,
            'trackId -> trackId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("savePlaylistTrackRelation: " + e.getMessage)
    }
  }

  def saveTrackAndArtistRelation(track: Track, id: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( s"""INSERT into tracks(name)
        values ({name})"""
        ).on(
            'name -> track.name
          ).executeInsert() match {
          case Some(x: Long) => saveArtistTrackRelation(id, x)
          case _ => None
        }
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create track: " + e.getMessage)
    }
  }

  def saveArtistTrackRelation(artistId: Long, trackId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT INTO artistsTracks (artistId, trackId)
            VALUES ({artistId}, {trackId})""").on(
            'artistId -> artistId,
            'trackId -> trackId
          ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("saveArtistTrackRelation: " + e.getMessage)
    }
  }

  def deleteTrack(trackId: Long): Long = {
    try {
      DB.withConnection { implicit connection =>
        SQL("""DELETE FROM tracks WHERE trackId={trackId}""").on('trackId -> trackId).executeUpdate()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot delete track: " + e.getMessage)
    }
  }

  def followTrack(userId : Long, trackId : Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("insert into trackFollowed(userId, trackId) values ({userId}, {trackId})").on(
          'userId -> userId,
          'trackId -> trackId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow track: " + e.getMessage)
    }
  }
}