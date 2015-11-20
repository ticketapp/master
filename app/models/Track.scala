package models

import java.sql.BatchUpdateException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import javax.inject.Inject
import scala.language.implicitConversions
import json.JsonHelper._

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.iteratee.{Iteratee, Enumeratee, Enumerator}
import play.api.libs.json.{Json, JsValue}
import services.MyPostgresDriver.api._
import services.{DistinctBy, FollowService, MyPostgresDriver, Utilities}

import scala.collection.IterableLike
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

case class Track (uuid: UUID,
                  title: String, 
                  url: String, 
                  platform: Char,
                  thumbnailUrl: String,
                  artistFacebookUrl: String,
                  artistName: String,
                  redirectUrl: Option[String] = None,
                  confidence: Double = 0.toDouble)

case class TrackWithGenres(track: Track, genres: Seq[Genre])

class TrackMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             val utilities: Utilities)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with FollowService {


  def findAllByArtist(artistFacebookUrl: String, numberToReturn: Int, offset: Int): Future[Seq[Track]] = {
    val query = tracks
      .filter(_.artistFacebookUrl === artistFacebookUrl)
      .sortBy(_.confidence.desc)

    numberToReturn match {
      case 0 =>
        db.run(query.result)
      case strictlyPositiveNumberToReturn if strictlyPositiveNumberToReturn > 0 =>
        db.run(query.drop(offset).take(numberToReturn).result)
      case _ =>
        Logger.error("Track.findAllByArtist: impossible to return a negative number of tracks")
        Future(Seq.empty)
    }
  }

  def findByPlaylistId(playlistId: Long): Future[Seq[TrackWithPlaylistRank]] = {
    val query = for {
      playlistTrack <- playlistsTracks if playlistTrack.playlistId === playlistId
      track <- tracks if track.uuid === playlistTrack.trackId
    } yield (track, playlistTrack.trackRank)

    db.run(query.result) map { trackWithPlaylistRankTuple =>
      trackWithPlaylistRankTuple map TrackWithPlaylistRank.tupled
    } map(_.sortBy(_.rank))
  }

  def find(uuid: UUID): Future[Option[Track]] = db.run(tracks.filter(_.uuid === uuid).result.headOption)

  def findAllContainingInTitle(pattern: String): Future[Seq[Track]] = {
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      track <- tracks if track.title.toLowerCase like s"%$lowercasePattern%"
    } yield track

    db.run(query.take(10).result)
  }

  def findAllByGenre(genreName: String, numberToReturn: Int, offset: Int): Future[Seq[Track]] = {
    val query = for {
      genre <- genres if genre.name === genreName
      trackGenre <- tracksGenres if trackGenre.genreId === genre.id
      track <- tracks if track.uuid === trackGenre.trackId
    } yield track

    db.run(query.drop(offset).take(numberToReturn).result) map { _.toVector }
  }

  def save(track: Track): Future[Track] = db.run((for {
    trackFound <- tracks.filter(trackFound => trackFound.title === track.title &&
      trackFound.artistName === track.artistName || trackFound.url === track.url).result.headOption
    _ <- artists.filter(_.facebookUrl === track.artistFacebookUrl).map(_.hasTracks).update(true)
    result <- trackFound.map(DBIO.successful).getOrElse(tracks returning tracks.map(_.uuid) += track)
  } yield result match {
      case t: Track =>
        Logger.info("Track already in database: " + t)
        t
      case uuid: UUID =>
        track.copy(uuid = uuid)
  }).transactionally)

  def saveSequence(tracksToSave: Set[Track]): Future[Any] = {
    val artistFacebookUrls = tracksToSave map (_.artistFacebookUrl)

    db.run(tracks ++= tracksToSave) map { _ =>
      artistFacebookUrls map { facebookUrl =>
        db.run(artists.filter(_.facebookUrl === facebookUrl).map(_.hasTracks).update(true))
      }
    } recover {
      case psqlException: BatchUpdateException =>
        Logger.error("Track.saveSequence:\nBatchUpdateException:" + psqlException.getNextException)
      case NonFatal(e) =>
        Logger.error("Track.saveSequence:\nMessage:", e)
    }
  }

  def delete(uuid: UUID): Future[Int] = db.run(tracks.filter(_.uuid === uuid).delete)

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

  val lockFilterDuplicateTracks: ReentrantLock = new ReentrantLock()

  def filterDuplicateTracksEnumerator(tracksEnumerator: Enumerator[Set[Track]]): Enumerator[Set[Track]] = {

    var bufferTrackUrls: ListBuffer[String] = ListBuffer.empty
    case class ArtistNameAndTrackTitle(artistName: String, trackTitle: String)
    var bufferTupleArtistNameTitle: ListBuffer[ArtistNameAndTrackTitle] = ListBuffer.empty

    val filterDuplicateTracksEnumeratee: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks =>

      implicit def toRich[A, Repr](xs: IterableLike[A, Repr]): DistinctBy[A, Repr] = new DistinctBy(xs)

      try {
        if (lockFilterDuplicateTracks.tryLock(3, TimeUnit.SECONDS)) {
          val notDuplicateTracksFromThisSet: Set[Track] =
            removeDuplicateByTitleAndArtistName(tracks.toSeq).distinctBy(_.url).toSet

          val notDuplicateTracksComparedToBuffer: Set[Track] = notDuplicateTracksFromThisSet.filterNot(t =>
            bufferTrackUrls.contains(t.url) ||
              bufferTupleArtistNameTitle.contains(ArtistNameAndTrackTitle(t.artistName, t.title)))

          bufferTrackUrls ++= (notDuplicateTracksComparedToBuffer map (_.url))
          bufferTupleArtistNameTitle ++= (notDuplicateTracksComparedToBuffer map (track => ArtistNameAndTrackTitle(
            artistName = track.artistName,
            trackTitle = track.title)))

          notDuplicateTracksComparedToBuffer
        } else
          Set.empty
      } finally {
        lockFilterDuplicateTracks.unlock()
      }
    }

    tracksEnumerator &> filterDuplicateTracksEnumeratee
  }

  def toJsonEnumeratee: Enumeratee[Set[Track], JsValue] = Enumeratee.map[Set[Track]](tracks => Json.toJson(tracks))

  def saveTracksInFutureEnumeratee: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks =>
    Future(saveSequence(tracks))
    tracks
  }

  def toTracksWithDelay: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks: Set[Track] =>
    Thread.sleep(1000)
    tracks
  }

  def saveEnumeratorWithDelay(tracksEnumerator: Enumerator[Set[Track]]): Unit = {
    val tracksEnumeratorWithoutDuplicates = filterDuplicateTracksEnumerator(tracksEnumerator)

    tracksEnumeratorWithoutDuplicates |>> toTracksWithDelay &>> Iteratee.foreach { tracks =>
      saveSequence(tracks)
    }
  }
}