package tracksDomain

import java.sql.BatchUpdateException
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

import actors.DuplicateTracksActor.FilterTracks
import actors.DuplicateTracksActorInstance
import akka.pattern.ask
import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import genresDomain.Genre
import json.JsonHelper._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json}
import playlistsDomain.TrackWithPlaylistRank
import services.{FollowService, Utilities}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
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
                             duplicateTracksActorInstance: DuplicateTracksActorInstance)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with FollowService
    with Utilities {


  implicit val timeout: akka.util.Timeout = 5.seconds

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
      artistName = replaceAccentuatedLetters(track.artistName)
      trackTitle = replaceAccentuatedLetters(track.title)
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
    replaceAccentuatedLetters(trackTitle.toLowerCase) contains
      replaceAccentuatedLetters(artistName.toLowerCase)

  def normalizeTrackTitle(title: String, artistName: String): String =
    ("""(?i)""" + Pattern.quote(artistName) + """\s*[:/-]?\s*""").r.replaceFirstIn(
      """(?i)(\.wm[a|v]|\.ogc|\.amr|\.wav|\.flv|\.mov|\.ram|\.mp[3-5]|\.pcm|\.alac|\.eac-3|\.flac|\.vmd)\s*$""".r
        .replaceFirstIn(title, ""),
      "")

  def filterDuplicateTracksEnumeratee: Enumeratee[Set[Track], Set[Track]] = Enumeratee.mapM[Set[Track]] { tracks =>
    (duplicateTracksActorInstance.duplicateTracksActor ? FilterTracks(tracks)).mapTo[Set[Track]].map { withoutDuplicateTracks =>
      withoutDuplicateTracks
    }
  }

  def toJsonEnumeratee: Enumeratee[Set[Track], JsValue] = Enumeratee.map[Set[Track]](tracks => Json.toJson(tracks))

  def saveTracksInFutureEnumeratee: Enumeratee[Set[Track], Set[Track]] = Enumeratee.map[Set[Track]] { tracks =>
    Future(saveSequence(tracks))
    tracks
  }

  def saveTracksEnumerator(tracksEnumerator: Enumerator[Set[Track]]): Unit = {
    tracksEnumerator &> filterDuplicateTracksEnumeratee |>> Iteratee.foreach { tracks =>
      saveSequence(tracks)
    }
  }
}