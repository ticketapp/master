package models

import models.Playlist.TrackIdAndRank
import play.api.Logger
import services.Utilities.columnToChar
import json.JsonHelper.trackWrites
import play.api.libs.json.DefaultWrites
import anorm.SqlParser._
import anorm._
import controllers.DAOException
import play.api.db.DB
import play.api.Play.current
import java.util.Date
import java.math.BigDecimal
import anorm.Column.rowToBigDecimal
import scala.util.Success
import scala.util.Try

case class Track (trackId: Option[Long],
                  title: String, 
                  url: String, 
                  platform: Char,
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  redirectUrl: Option[String] = None,
                  confidence: Option[Int] = None,
                  playlistRank: Option[Float] = None)

object Track {

  def formApplyForTrackCreatedWithArtist(title: String, url: String, platform: String, thumbnailUrl: Option[String],
  userThumbnailUrl: Option[String], artistFacebookUrl: String, redirectUrl: Option[String]): Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) => new Track(None, title, url, platform(0), thumbnail, artistFacebookUrl, redirectUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) =>
          new Track(None, title, url, platform(0), userThumbnail, artistFacebookUrl, redirectUrl)
        case None =>
          throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }

  def formUnapplyForTrackCreatedWithArtist(track: Track) = Some((track.title, track.url, track.platform.toString,
    Some(track.thumbnailUrl), None, track.artistFacebookUrl, track.redirectUrl))

  def formApply(title: String, url: String, platform: String, thumbnailUrl: String, artistFacebookUrl: String,
                redirectUrl: Option[String]): Track =
   new Track(None, title, url, platform(0), thumbnailUrl, artistFacebookUrl, redirectUrl)
  def formUnapply(track: Track) =
    Some((track.title, track.url, track.platform.toString, track.thumbnailUrl, track.artistFacebookUrl, track.redirectUrl))

  private val trackParser: RowParser[Track] = {
    get[Long]("trackId") ~
      get[String]("title") ~
      get[String]("url") ~
      get[Char]("platform") ~
      get[String]("thumbnailUrl") ~
      get[String]("artistFacebookUrl") ~
      get[Option[String]]("redirectUrl") ~
      get[Option[Int]]("confidence") map {
      case trackId ~ title ~ url ~ platform ~ thumbnailUrl ~ artistFacebookUrl ~ redirectUrl ~ confidence =>
        Track(Option(trackId), title, url, platform, thumbnailUrl, artistFacebookUrl, redirectUrl, confidence)
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
        """SELECT tracks.*, playlistsTracks.trackRank FROM tracks tracks
          |  INNER JOIN playlistsTracks playlistsTracks
          |    ON tracks.trackId = playlistsTracks.trackId
          |  INNER JOIN playlists playlists
          |    ON playlists.playlistId = playlistsTracks.playlistId
          |  WHERE playlists.playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .as(trackParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Track.findTracksIdByPlaylistId: " + e.getMessage)
  }

  private val trackIdAndRankParser: RowParser[(Long, java.math.BigDecimal)] = {
    get[Long]("trackId") ~
      get[java.math.BigDecimal]("trackRank") map {
      case trackId ~ trackRank => (trackId, trackRank)
    }
  }

  def find(trackId: Long): Option[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks WHERE trackId = {trackId}")
        .on('trackId -> trackId)
        .as(trackParser.singleOpt)
    }
  }

  def findAllContaining(pattern: String): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks WHERE LOWER(title) LIKE '%'||{patternLowCase}||'%' LIMIT 10")
        .on('patternLowCase -> pattern.toLowerCase)
        .as(trackParser.*)
    }
  } catch {
    case e: Exception => throw new DAOException("Track.findAllContaining: " + e.getMessage)
  }

  def save(track: Track): Try[Option[Long]] = Try {
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
  }

  def savePlaylistTrackRelation(playlistId: Long, trackIdAndRank: TrackIdAndRank): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO playlistsTracks (playlistId, trackId, trackRank)
          | VALUES ({playlistId}, {trackId}, {trackRank})""".stripMargin)
        .on(
          'playlistId -> playlistId,
          'trackId -> trackIdAndRank.id,
          'trackRank -> trackIdAndRank.rank.bigDecimal)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Track.savePlaylistTrackRelation: " + e.getMessage)
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

  def delete(trackId: Long): Int = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracks WHERE trackId = {trackId}""".stripMargin)
        .on('trackId -> trackId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Track.delete: " + e.getMessage)
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

  def upsertRatingUp(userId: String, trackId: Long, rating: Int): Try[Boolean] = Try {
    getRating(trackId) match {
      case Success(Some(actualRating)) => updateConfidence(trackId, calculateConfidence(actualRating, rating))
      case _ => Logger.error(s"Track.upsertRatingUp: rating up error with trackId: trackId and userId: $userId")
    }

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingUp({userId}, {trackId}, {rating})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> rating)
        .execute()
    }
  }

  def upsertRatingDown(userId: String, trackId: Long, rating: Int): Try[Boolean] = Try {
    getRating(trackId) match {
      case Success(Some(actualRating)) => updateConfidence(trackId, calculateConfidence(actualRating, rating))
      case _ => Logger.error(s"Track.upsertRatingDown: rating down error with trackId: trackId and userId: $userId")
    }

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingDown({userId}, {trackId}, {rating})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> math.abs(rating))
        .execute()
    }
  }

  private val ratingParser: RowParser[String] = {
    get[Option[Int]]("ratingUp") ~
      get[Option[Int]]("ratingDown") map {
      case ratingUp ~ ratingDown => ratingUp.getOrElse(0).toString + "," + ratingDown.getOrElse(0).toString
    }
  }

  def getRating(trackId: Long): Try[Option[String]] = Try {
    DB.withConnection { implicit connection =>
      SQL("SELECT ratingUp, ratingDown FROM tracks WHERE trackId = {trackId}")
        .on('trackId -> trackId)
        .as(ratingParser.singleOpt)
    }
  }

  def updateConfidence(trackId: Long, confidence: Int): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
      s"""UPDATE tracks
        | SET confidence = $confidence
        | WHERE trackId = $trackId""".stripMargin)
      .executeUpdate()
    }
  }

  def getRatingForUser(userId: String, trackId: Long): Try[Option[String]] = Try {
    DB.withConnection { implicit connection =>
      SQL("SELECT ratingUp, ratingDown FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}")
        .on(
          'userId -> userId,
          'trackId -> trackId)
        .as(ratingParser.singleOpt)
    }
  }

  def deleteRatingForUser(userId: String, trackId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}""".stripMargin)
        .on('userId -> userId,
            'trackId -> trackId)
        .executeUpdate()
    }
  }

  def addToFavorites(userId: String, trackId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO usersFavoriteTracks(userId, trackId) VALUES({userId}, {trackId})")
        .on(
          'userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }

  def removeFromFavorites(userId: String, trackId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM usersFavoriteTracks WHERE userId = {userId} AND trackId = {trackId}""")
        .on('userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }

  def findFavorites(userId: String): Try[Seq[Track]] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT tracks.* FROM tracks tracks
          |  INNER JOIN usersFavoriteTracks usersFavoriteTracks
          |    ON tracks.trackId = usersFavoriteTracks.trackId
          |WHERE usersFavoriteTracks.userId = {userId}""".stripMargin)
        .on('userId -> userId)
        .as(trackParser.*)
    }
  }

  def calculateConfidence(actualRating: String, newRate: Int): Int = {
    val actualRatingSplit = actualRating.split(",")
    var ups = actualRatingSplit(0).toFloat
    var downs = actualRatingSplit(1).toFloat
    newRate match {
      case ratingUp if ratingUp > 0 => ups = ups + ratingUp
      case ratingDown if ratingDown <= 0 => downs = downs + math.abs(ratingDown)
    }

    if (ups == 0)
      (-downs).toInt
    else {
      val n = ups + downs
      val z = 1.64485
      val phat = ups / n

      val confidence = (phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)
      (confidence * 1000000).toInt
    }
  }
}