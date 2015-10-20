package models

import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import services.MyPostgresDriver.api._
import controllers.DAOException
import play.api.Logger

import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import services.{MyPostgresDriver, Utilities}
import slick.model.ForeignKeyAction

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class Track (uuid: UUID,
                  title: String, 
                  url: String, 
                  platform: Char,
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  artistName: String,
                  redirectUrl: Option[String] = None,
                  confidence: Option[Double] = Option(0.toDouble)/*,
                  playlistRank: Option[Double] = None,
                  genres: Seq[Genre] = Seq.empty*/)

class TrackMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def formApplyForTrackCreatedWithArtist(trackId: String, title: String, url: String, platform: String,
                                         thumbnailUrl: Option[String], userThumbnailUrl: Option[String],
                                         artistFacebookUrl: String, artistName: String, redirectUrl: Option[String])
  : Track = { thumbnailUrl match {
      case Some(thumbnail: String) =>
        Track(UUID.fromString(trackId), title, url, platform(0), thumbnail, artistFacebookUrl, artistName, redirectUrl)
      case None => userThumbnailUrl match {
        case Some(userThumbnail: String) =>
          new Track(UUID.fromString(trackId), title, url, platform(0), userThumbnail, artistFacebookUrl, artistName, redirectUrl)
        case None =>
          throw new Exception("A track must have a thumbnail or a user Thumbnail url to be saved")
      }
    }
  }

  def formUnapplyForTrackCreatedWithArtist(track: Track) = Some((track.uuid.toString, track.title, track.url,
    track.platform.toString, Some(track.thumbnailUrl), None, track.artistFacebookUrl, track.artistName: String,
    track.redirectUrl))

  def formApply(trackId: String, title: String, url: String, platform: String, thumbnailUrl: String,
                artistFacebookUrl: String, artistName: String, redirectUrl: Option[String]): Track =
   new Track(UUID.fromString(trackId), title, url, platform(0), thumbnailUrl, artistFacebookUrl, artistName, redirectUrl)
  def formUnapply(track: Track) =
    Some((track.uuid.toString, track.title, track.url, track.platform.toString, track.thumbnailUrl,
      track.artistFacebookUrl, track.artistName, track.redirectUrl))

  def findAll: Future[Seq[Track]] = db.run(tracks.result)

  def findAllByArtist(artistFacebookUrl: String, numberToReturn: Int, offset: Int): Future[Seq[Track]] = {
    val query = tracks
      .filter(_.artistFacebookUrl === artistFacebookUrl)
      .sortBy(_.confidence.desc)
    val queryWithNumberToReturnAndOffset = query.drop(offset).take(numberToReturn)

    numberToReturn match {
      case 0 => db.run(query.result)
      case strictlyPositiveNumberToReturn if strictlyPositiveNumberToReturn > 0 =>
        db.run(queryWithNumberToReturnAndOffset.result)
      case _ =>
        Logger.error("Track.findAllByArtist: impossible to return a negative number of tracks")
        Future { Seq.empty }
    }
  }

  def findByPlaylistId(playlistId: Option[Long]): Future[Seq[Track]] = {
    Future { Seq.empty }
  }


//    try {
//    DB.withConnection { implicit connection =>
//      SQL(
//        """SELECT tracks.*, playlistsTracks.trackRank FROM tracks tracks
//          |  INNER JOIN playlistsTracks playlistsTracks
//          |    ON tracks.trackId = playlistsTracks.trackId
//          |  INNER JOIN playlists playlists
//          |    ON playlists.playlistId = playlistsTracks.playlistId
//          |  WHERE playlists.playlistId = {playlistId}
//          |    ORDER BY trackRank""".stripMargin)
//        .on('playlistId -> playlistId)
//        .as(trackWithPlaylistRankParser.*)
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Track.findTracksIdByPlaylistId: " + e.getMessage)
//  }

//  private val trackIdAndRankParser: RowParser[(Long, java.math.BigDecimal)] = {
//    get[Long]("trackId") ~
//      get[java.math.BigDecimal]("trackRank") map {
//      case trackId ~ trackRank => (trackId, trackRank)
//    }
//  }

  def find(uuid: UUID): Future[Option[Track]] = {
    db.run(tracks.filter(_.uuid === uuid).result.headOption)
  }

  def findAllContaining(pattern: String): Future[Seq[Track]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      track <- tracks if track.title.toLowerCase like s"%$lowercasePattern%"
    } yield track

    db.run(query.take(10).result)
  }

  def findByGenre(genre: String, numberToReturn: Int, offset: Int): Future[Seq[Track]] = {
    Future { Seq.empty }
    //    Try {
    //    DB.withConnection { implicit connection =>
    //      SQL(
    //        s"""SELECT a.*
    //           |FROM tracksGenres aG
    //           |  INNER JOIN tracks a ON a.trackId = aG.trackId
    //           |  INNER JOIN genres g ON g.genreId = aG.genreId
    //           |WHERE g.name = {genre}
    //           |LIMIT $numberToReturn OFFSET $offset""".stripMargin)
    //        .on('genre -> genre)
    //        .as(trackParser.*)
    //    }
    //  }
  }

  /*
    def save(organizer: Organizer): Future[Organizer] = {
    val insertQuery = organizers returning organizers.map(_.id) into ((organizer, id) =>
      organizer.copy(id = Option(id)))

    val action = insertQuery += organizer

    db.run(action)
  }
   */

  def save(track: Track): Future[Track] = db.run((for {
    trackFound <- tracks.filter(trackFound => (trackFound.title === track.title &&
      trackFound.artistName === track.artistName) || trackFound.url === track.url).result.headOption
    result <- trackFound.map(DBIO.successful).getOrElse(tracks returning tracks.map(_.uuid) += track)
  } yield result match {
      case t: Track => t
      case uuid: UUID => track.copy(uuid = uuid)
  }).transactionally)

  def delete(uuid: UUID): Future[Int] = db.run(tracks.filter(_.uuid === uuid).delete)


  def followTrack(userId : Long, trackId : UUID): Option[Long] = {
//    DB.withConnection { implicit connection =>
//      SQL("INSERT INTO trackFollowed(userId, trackId) VALUES ({userId}, {trackId})")
//        .on(
//          'userId -> userId,
//          'trackId -> trackId)
//        .executeInsert()
//    }
//  } catch {
//    case e: Exception => throw new DAOException("Cannot follow track: " + e.getMessage)
    None
  }
/*
  def addToFavorites(userId: UUID, trackId: UUID): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO usersFavoriteTracks(userId, trackId) VALUES({userId}, {trackId})")
        .on(
          'userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }

  def removeFromFavorites(userId: UUID, trackId: UUID): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL("""DELETE FROM usersFavoriteTracks WHERE userId = {userId} AND trackId = {trackId}""")
        .on('userId -> userId,
          'trackId -> trackId)
        .executeUpdate()
    }
  }*/
//
//  def findFavorites(userId: UUID): Try[Seq[Track]] = Try {
//    DB.withConnection { implicit connection =>
//      SQL(
//        """SELECT tracks.* FROM tracks tracks
//          |  INNER JOIN usersFavoriteTracks usersFavoriteTracks
//          |    ON tracks.trackId = usersFavoriteTracks.trackId
//          |WHERE usersFavoriteTracks.userId = {userId}""".stripMargin)
//        .on('userId -> userId)
//        .as(trackParser.*)
//    }
//  }

  def removeDuplicateByTitleAndArtistName(tracks: Seq[Track]): Seq[Track] = {
    var tupleArtistNameTitle = new ListBuffer[(String, String)]()
    for {
      track <- tracks
      artistName = utilities.replaceAccentuatedLetters(track.artistName)
      trackTitle = utilities.replaceAccentuatedLetters(track.title)
      if !tupleArtistNameTitle.contains((artistName, trackTitle))
    } yield {
      tupleArtistNameTitle += ((artistName, trackTitle))
      track
    }
  }

  def calculateConfidence(actualRatingUp: Int, actualRatingDown: Int): Double = {
    val up = actualRatingUp.toDouble / 1000
    val down = actualRatingDown.toDouble / 1000

    if (up == 0)
      -down
    else {
      val n = up + down
      val z = 1.64485
      val phat = up / n

      (phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)
    }
  }

  def isArtistNameInTrackTitle(trackTitle: String, artistName: String): Boolean =
    utilities.replaceAccentuatedLetters(trackTitle.toLowerCase) contains
      utilities.replaceAccentuatedLetters(artistName.toLowerCase)

  def normalizeTrackTitle(title: String, artistName: String): String =
    ("""(?i)""" + Pattern.quote(artistName) + """\s*[:/-]?\s*""").r.replaceFirstIn(
      """(?i)(\.wm[a|v]|\.ogc|\.amr|\.wav|\.flv|\.mov|\.ram|\.mp[3-5]|\.pcm|\.alac|\.eac-3|\.flac|\.vmd)\s*$""".r
        .replaceFirstIn(title, ""),
      "")
}