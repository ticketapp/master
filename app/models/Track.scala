package models

import models.Playlist.TrackIdAndRank
import play.api.Logger
import services.Utilities.{columnToChar}
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
import scala.util.{Try, Success, Failure}

case class Track (trackId: String,
                  title: String, 
                  url: String, 
                  platform: Char,
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  artistName: String,
                  redirectUrl: Option[String] = None,
                  confidence: Option[Double] = None,
                  playlistRank: Option[Float] = None)

object Track {

  def formApplyForTrackCreatedWithArtist(trackId: String, title: String, url: String, platform: String,
                                         thumbnailUrl: Option[String], userThumbnailUrl: Option[String],
                                         artistFacebookUrl: String, artistName: String, redirectUrl: Option[String])
  : Track = {
    thumbnailUrl match {
      case Some(thumbnail: String) =>
        Track(trackId, title, url, platform(0), thumbnail, artistFacebookUrl, artistName, redirectUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) =>
          new Track(trackId, title, url, platform(0), userThumbnail, artistFacebookUrl, artistName, redirectUrl)
        case None =>
          throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }

  def formUnapplyForTrackCreatedWithArtist(track: Track) = Some((track.trackId, track.title, track.url,
    track.platform.toString, Some(track.thumbnailUrl), None, track.artistFacebookUrl, track.artistName: String,
    track.redirectUrl))

  def formApply(trackId: String, title: String, url: String, platform: String, thumbnailUrl: String,
                artistFacebookUrl: String, artistName: String, redirectUrl: Option[String]): Track =
   new Track(trackId, title, url, platform(0), thumbnailUrl, artistFacebookUrl, artistName, redirectUrl)
  def formUnapply(track: Track) =
    Some((track.trackId, track.title, track.url, track.platform.toString, track.thumbnailUrl,
      track.artistFacebookUrl, track.artistName, track.redirectUrl))

  private val trackParser: RowParser[Track] = {
    get[String]("trackId") ~
      get[String]("title") ~
      get[String]("url") ~
      get[Char]("platform") ~
      get[String]("thumbnailUrl") ~
      get[String]("artistFacebookUrl") ~
      get[String]("artistName") ~
      get[Option[String]]("redirectUrl") ~
      get[Double]("confidence") map {
      case trackId ~ title ~ url ~ platform ~ thumbnailUrl ~ artistFacebookUrl ~ artistName ~ redirectUrl ~
        confidence => Track(trackId, title, url, platform, thumbnailUrl, artistFacebookUrl, artistName,
        redirectUrl, Option(confidence))
    }
  }

  def findAll: Seq[Track] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tracks")
        .as(trackParser.*)
    }
  }

  def findAllByArtist(artistFacebookUrl: String, numberToReturn: Int, offset: Int): Seq[Track] = try {
    DB.withConnection { implicit connection =>
      numberToReturn match {
        case 0 =>
          SQL(
            """SELECT * FROM tracks
              |  WHERE artistFacebookUrl = {artistFacebookUrl}
              |  ORDER BY confidence DESC""".stripMargin)
            .on('artistFacebookUrl -> artistFacebookUrl)
            .as(trackParser.*)
        case n =>
          SQL(
            s"""SELECT * FROM tracks
              |  WHERE artistFacebookUrl = {artistFacebookUrl}
              |  ORDER BY confidence DESC
              |  LIMIT $n OFFSET $offset""".stripMargin)
            .on('artistFacebookUrl -> artistFacebookUrl)
            .as(trackParser.*)
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Track.findAllByArtist: " + e.getMessage)
  }

  def findByPlaylistId(playlistId: Option[Long]): Seq[Track] = try {
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

  def find(trackId: String): Try[Option[Track]] = Try {
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

  def save(track: Track): Try[Boolean] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT insertTrack({trackId}, {title}, {url}, {platform}, {thumbnailUrl}, {artistFacebookUrl},
          |{artistName}, {redirectUrl})""".stripMargin)
        .on(
          'trackId -> track.trackId,
          'title -> track.title,
          'url -> track.url,
          'platform -> track.platform,
          'thumbnailUrl -> track.thumbnailUrl,
          'artistFacebookUrl -> track.artistFacebookUrl,
          'artistName -> track.artistName,
          'redirectUrl -> track.redirectUrl)
        .execute()
    }
  }

  def delete(trackId: String): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracks WHERE trackId = {trackId}""".stripMargin)
        .on('trackId -> trackId)
        .executeUpdate()
    }
  }

  def followTrack(userId : Long, trackId : String): Option[Long] = try {
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

  def upsertRatingUp(userId: String, trackId: String, rating: Int): Try[Boolean] = Try {
    updateRating(trackId, rating)

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingUp({userId}, {trackId}, {rating})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> rating)
        .execute()
    }
  }

  def upsertRatingDown(userId: String, trackId: String, rating: Int): Try[Boolean] = Try {
    updateRating(trackId, rating)

    DB.withConnection { implicit connection =>
      SQL("SELECT upsertTrackRatingDown({userId}, {trackId}, {rating})")
        .on(
          'userId -> userId,
          'trackId -> trackId,
          'rating -> math.abs(rating))
        .execute()
    }
  }

  private val ratingParser: RowParser[(Int, Int)] = {
    get[Option[Int]]("ratingUp") ~
      get[Option[Int]]("ratingDown") map {
      case ratingUp ~ ratingDown => (ratingUp.getOrElse(0), ratingDown.getOrElse(0))
    }
  }

  def getRating(trackId: String): Try[Option[(Int, Int)]] = Try {
    DB.withConnection { implicit connection =>
      SQL("SELECT ratingUp, ratingDown FROM tracks WHERE trackId = {trackId}")
        .on('trackId -> trackId)
        .as(ratingParser.singleOpt)
    }
  }

  def updateRating(trackId: String, ratingToAdd: Int): Try[Double] = Try {
    getRating(trackId) match {
      case Success(Some(actualRating)) =>
        var actualRatingUp = actualRating._1
        var actualRatingDown = actualRating._2

        ratingToAdd match {
          case ratingUp if ratingUp > 0 => actualRatingUp = actualRatingUp + ratingUp
          case ratingDown if ratingDown <= 0 => actualRatingDown = actualRatingDown + math.abs(ratingDown)
        }

        val confidence = calculateConfidence(actualRatingUp, actualRatingDown)

        persistUpdateRating(trackId, actualRatingUp, actualRatingDown, confidence) match {
          case Success(1) =>
            confidence
          case Failure(exception) =>
            Logger.error(s"Track.updateRating: persistUpdateRating: error while updating with trackId: trackId", exception)
            throw exception
          case _ =>
            throw new DAOException("Track.updateRating: persistUpdateRating")
        }

      case Failure(exception) =>
        Logger.error(s"Track.updateRating: error while updating with trackId: trackId", exception)
        throw exception

      case _ =>
        Logger.error(s"Track.updateRating: error while updating with trackId: $trackId")
        throw new DAOException("Track.updateRating")
    }
  }

  def persistUpdateRating(trackId: String, actualRatingUp: Int, actualRatingDown: Int, confidence: Double)
  : Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        s"""UPDATE tracks
           |  SET ratingUp = $actualRatingUp, ratingDown = $actualRatingDown, confidence = $confidence
           |    WHERE trackId = {trackId}""".stripMargin)
        .on('trackId -> trackId)
        .executeUpdate()
    }
  }

  def getRatingForUser(userId: String, trackId: String): Try[Option[(Int, Int)]] = Try {
    DB.withConnection { implicit connection =>
      SQL("SELECT ratingUp, ratingDown FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}")
        .on(
          'userId -> userId,
          'trackId -> trackId)
        .as(ratingParser.singleOpt)
    }
  }

  def deleteRatingForUser(userId: String, trackId: String): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM tracksRating WHERE userId = {userId} AND trackId = {trackId}""".stripMargin)
        .on('userId -> userId,
            'trackId -> trackId)
        .executeUpdate()
    }
  }

  def addToFavorites(userId: String, trackId: String): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO usersFavoriteTracks(userId, trackId) VALUES({userId}, {trackId})")
        .on(
          'userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }

  def removeFromFavorites(userId: String, trackId: String): Try[Int] = Try {
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

  def calculateConfidence(actualRatingUp: Int, actualRatingDown: Int): Double = {
    val up = actualRatingUp.toDouble
    val down = actualRatingDown.toDouble

    if (up == 0)
      -down
    else {
      val n = up + down
      val z= 1.64485
      val phat = up / n

      (phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)
    }
  }
}