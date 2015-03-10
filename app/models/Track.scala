package models

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
                  thumbnailUrl: String)

object Track {
  implicit val trackWrites = Json.writes[Track]

  def formApply(title: String, url: String, platform: String, thumbnailUrl: Option[String],
  userThumbnailUrl: Option[String]): Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) => new Track(-1L, title, url, platform, thumbnail)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) => new Track(-1L, title, url, platform, userThumbnail)
        case None => throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }
  def formUnapply(track: Track) = Some((track.title, track.url, track.platform, Some(track.thumbnailUrl), None))


  private val TrackParser: RowParser[Track] = {
    get[Long]("trackId") ~
      get[String]("title") ~
      get[String]("url") ~
      get[String]("platform") ~
      get[String]("thumbnailUrl") map {
      case trackId ~ title ~ url ~ platform ~ thumbnailUrl=>
        Track(trackId, title, url, platform, thumbnailUrl)
    }
  }

  def findAll(): Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("select * from tracks")
        .as(TrackParser.*)
    }
  }

  def findAllByArtist(artistId: Long): Set[Track] = {
    DB.withConnection { implicit connection =>
      SQL("""SELECT *
             FROM artistsTracks aT
             INNER JOIN tracks t ON t.trackId = aT.trackId where aT.artistId = {artistId}""")
        .on('artistId -> artistId)
        .as(TrackParser.*)
        .toSet
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
        SQL("SELECT * FROM tracks WHERE LOWER(title) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
          .on('patternLowCase -> pattern.toLowerCase)
          .as(TrackParser.*)
      }
    } catch {
      case e: Exception => throw new DAOException("Problem with the method Track.findAllContaining: " + e.getMessage)
    }
  }

  def saveTrackAndArtistRelation(track: Track, artistIdOrArtistFacebookUrl: Either[Long, String]): Option[Long] = {
    save(track) match {
      case Some(trackId: Long) => saveArtistTrackRelation(artistIdOrArtistFacebookUrl, trackId)
      case _ => None
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
        SQL( """INSERT into tracks(title, url, platform, thumbnail)
        VALUES ({title}, {url}, {platform}, {thumbnailUrl})""")
          .on(
            'title -> track.title,
            'url -> track.url,
            'platform -> track.platform,
            'thumbnailUrl -> track.thumbnailUrl)
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

  def saveArtistTrackRelation(artistIdOrArtistFacebookUrl: Either[Long, String], trackId: Long): Option[Long] = {
    try {
      artistIdOrArtistFacebookUrl match {
        case Left(artistId) =>
          DB.withConnection { implicit connection =>
            SQL("""INSERT INTO artistsTracks (artistId, trackId)
                VALUES ({artistId}, {trackId})""")
              .on(
                'artistId -> artistId,
                'trackId -> trackId)
              .executeInsert()
          }
        case Right(facebookUrl) =>
          DB.withConnection { implicit connection =>
            SQL("""INSERT INTO artistsTracks (facebookUrl, trackId)
                VALUES ({facebookUrl}, {trackId})""")
              .on(
                'facebookUrl -> facebookUrl,
                'trackId -> trackId)
              .executeInsert()
          }
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