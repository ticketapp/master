package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Track (trackId: Long, 
                  title: String, 
                  url: String, 
                  platform: String, 
                  thumbnailUrl: String,
                  artistFacebookUrl: String)

object Track {
  implicit val trackWrites = Json.writes[Track]

  def formApplyForTrackCreatedWithArtist(title: String, url: String, platform: String, thumbnailUrl: Option[String],
  userThumbnailUrl: Option[String], artistFacebookUrl: String): Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) => new Track(-1L, title, url, platform, thumbnail, artistFacebookUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) => new Track(-1L, title, url, platform, userThumbnail, artistFacebookUrl)
        case None => throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }
  def formUnapplyForTrackCreatedWithArtist(track: Track) =
    Some((track.title, track.url, track.platform, Some(track.thumbnailUrl), None, track.artistFacebookUrl))

  def formApply(title: String, url: String, platform: String, thumbnailUrl: String, artistFacebookUrl: String): Track =
   new Track(-1L, title, url, platform, thumbnailUrl, artistFacebookUrl)
  def formUnapply(track: Track) =
    Some((track.title, track.url, track.platform, track.thumbnailUrl, track.artistFacebookUrl))

  private val TrackParser: RowParser[Track] = {
    get[Long]("trackId") ~
      get[String]("title") ~
      get[String]("url") ~
      get[String]("platform") ~
      get[String]("thumbnailUrl") ~
      get[String]("artistFacebookUrl") map {
      case trackId ~ title ~ url ~ platform ~ thumbnailUrl ~ artistFacebookUrl =>
        Track(trackId, title, url, platform, thumbnailUrl, artistFacebookUrl)
    }
  }

  def findAll: Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks")
        .as(TrackParser.*)
    }
  }

  def findAllByArtist(artistId: Long): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM tracks
             WHERE artistId = {artistId}""")
        .on('artistId -> artistId)
        .as(TrackParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Problem with method Track.findAllByArtist: " + e.getMessage)
  }

  def findTracksByPlaylistId(playlistId: Long): Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM playlistsTracks pT
             INNER JOIN tracks t ON t.trackId = pT.trackId
             WHERE pT.eventId = {eventId}""")
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
        SQL("SELECT * FROM tracks WHERE LOWER(title) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(TrackParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Track.findAllContaining: " + e.getMessage)
    }
  }

  def saveTrackAndPlaylistRelation(track: Track, playlistId: Long): Option[Long] = {
    save(track) match {
      case Some(trackId: Long) => savePlaylistTrackRelation(playlistId, trackId)
      case _ => None
    }
  }

  def save(track: Track): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL( """INSERT into tracks(title, url, platform, thumbnailUrl, artistFacebookUrl)
        VALUES ({title}, {url}, {platform}, {thumbnailUrl}, {artistFacebookUrl})""")
          .on(
            'title -> track.title,
            'url -> track.url,
            'platform -> track.platform,
            'thumbnailUrl -> track.thumbnailUrl,
            'artistFacebookUrl -> track.artistFacebookUrl)
          .executeInsert()
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
            'trackId -> trackId)
          .executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("savePlaylistTrackRelation: " + e.getMessage)
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
        SQL("INSERT INTO trackFollowed(userId, trackId) VALUES ({userId}, {trackId})")
          .on(
            'userId -> userId,
            'trackId -> trackId)
          .executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot follow track: " + e.getMessage)
    }
  }
}