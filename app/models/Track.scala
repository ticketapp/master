package models

import anorm.SqlParser._
import anorm._
import controllers._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current

case class Track (trackId: Option[Long],
                  title: String, 
                  url: String, 
                  platform: String, 
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  redirectUrl: Option[String] = None,
                  rank: Option[Int] = None)

object Track {
  implicit val trackWrites = Json.writes[Track]

  def formApplyForTrackCreatedWithArtist(title: String, url: String, platform: String, thumbnailUrl: Option[String],
  userThumbnailUrl: Option[String], artistFacebookUrl: String, redirectUrl: Option[String]): Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) => new Track(None, title, url, platform, thumbnail, artistFacebookUrl, redirectUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) =>
          new Track(None, title, url, platform, userThumbnail, artistFacebookUrl, redirectUrl)
        case None =>
          throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }
  def formUnapplyForTrackCreatedWithArtist(track: Track) = Some((track.title, track.url, track.platform,
    Some(track.thumbnailUrl), None, track.artistFacebookUrl, track.redirectUrl))

  def formApply(title: String, url: String, platform: String, thumbnailUrl: String, artistFacebookUrl: String,
                redirectUrl: Option[String]): Track =
   new Track(None, title, url, platform, thumbnailUrl, artistFacebookUrl, redirectUrl)
  def formUnapply(track: Track) =
    Some((track.title, track.url, track.platform, track.thumbnailUrl, track.artistFacebookUrl, track.redirectUrl))

  private val trackParser: RowParser[Track] = {
    get[Long]("trackId") ~
      get[String]("title") ~
      get[String]("url") ~
      get[String]("platform") ~
      get[String]("thumbnailUrl") ~
      get[String]("artistFacebookUrl") ~
      get[Option[String]]("redirectUrl") ~
      get[Option[Int]]("rank") map {
      case trackId ~ title ~ url ~ platform ~ thumbnailUrl ~ artistFacebookUrl ~ redirectUrl ~ rank =>
        Track(Option(trackId), title, url, platform, thumbnailUrl, artistFacebookUrl, redirectUrl, rank)
    }
  }

  def findAll: Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks")
        .as(trackParser.*)
    }
  }

  def findAllByArtist(artistFacebookUrl: String): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT * FROM tracks WHERE artistFacebookUrl = {artistFacebookUrl}""")
        .on('artistFacebookUrl -> artistFacebookUrl)
        .as(trackParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Track.findAllByArtist: " + e.getMessage)
  }

  def findTracksByPlaylistId(playlistId: Option[Long]): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT tracks.* FROM playlistsTracks playlistsTracks
          | INNER JOIN tracks tracks
          |   ON tracks.trackId = playlistsTracks.trackId
          | INNER JOIN playlists playlists
          |   ON playlists.playlistId = playlistsTracks.playlistId
          |WHERE playlists.playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .as(trackParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Track.findTracksIdByPlaylistId: " + e.getMessage)
  }

  def find(trackId: Long): Option[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks WHERE trackId = {trackId}")
        .on('trackId -> trackId)
        .as(trackParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Track] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("SELECT * FROM tracks WHERE LOWER(title) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(trackParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Track.findAllContaining: " + e.getMessage)
    }
  }

  def save(track: Track): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL("""SELECT insertTrack({title}, {url}, {platform}, {thumbnailUrl}, {artistFacebookUrl}, {redirectUrl})""")
        .on(
          'title -> track.title,
          'url -> track.url,
          'platform -> track.platform,
          'thumbnailUrl -> track.thumbnailUrl,
          'artistFacebookUrl -> track.artistFacebookUrl,
          'redirectUrl -> track.redirectUrl)
        .as(scalar[Option[Long]].single)
    }
  } catch {
    case e: Exception => throw new DAOException("Track.save: " + e.getMessage)
  }

  def savePlaylistTrackRelation(playlistId: Long, trackId: Long): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO playlistsTracks (playlistId, trackId)
          | VALUES ({playlistId}, {trackId})""".stripMargin)
        .on(
          'playlistId -> playlistId,
          'trackId -> trackId)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("savePlaylistTrackRelation: " + e.getMessage)
  }

  def deletePlaylistTrackRelation(playlistId: Long, trackId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          | WHERE playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("savePlaylistTrackRelation: " + e.getMessage)
  }

  def deleteTrack(trackId: Long): Long = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracks WHERE trackId = {trackId}""".stripMargin)
        .on('trackId -> trackId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Track.deleteTrack: " + e.getMessage)
  }

  def followTrack(userId : Long, trackId : Long): Option[Long] = try {
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