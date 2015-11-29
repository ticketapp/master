package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import com.google.common.cache.{Cache, CacheBuilder}
import services.Utilities
import tracksDomain.Track

import scala.collection.mutable.ListBuffer


object DuplicateTracksActor {
  def props = Props[DuplicateTracksActor]

  case class FilterTracks(tracks: Set[Track])
  case class DoneForArtist(artistFacebookUrl: String)
}

class DuplicateTracksActor extends Actor with Utilities {
  import DuplicateTracksActor._

  case class TrackTitleAndArtistFacebookUrl(title: String, artistFacebookUrl: String)

  var artistFacebookUrlToTrackUrl: Cache[String, ListBuffer[String]] = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .maximumSize(60)
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .build()

  var artistFacebookUrlToTrackTitleAndArtistName: Cache[String, ListBuffer[TrackTitleAndArtistFacebookUrl]] =
    CacheBuilder.newBuilder()
      .concurrencyLevel(1)
      .maximumSize(60)
      .expireAfterWrite(30, TimeUnit.SECONDS)
      .build()

  def receive = {

    case FilterTracks(tracks: Set[Track]) if tracks.isEmpty =>
      sender() ! Set.empty

    case FilterTracks(tracks: Set[Track]) =>
      val tracksToReturn: Set[Track] = filterTracks(tracks)

      sender() ! tracksToReturn

    case _ =>
      sender() ! "Unknown request received"
  }

  def filterTracks(tracks: Set[Track]): Set[Track] = {
    val artistFacebookUrl = tracks.head.artistFacebookUrl

    if (artistFacebookUrlToTrackUrl.getIfPresent(artistFacebookUrl) == null)
      artistFacebookUrlToTrackUrl.put(artistFacebookUrl, ListBuffer.empty)
    if (artistFacebookUrlToTrackTitleAndArtistName.getIfPresent(artistFacebookUrl) == null)
      artistFacebookUrlToTrackTitleAndArtistName.put(artistFacebookUrl, ListBuffer.empty)

    val tracksToReturn = for {
      track <- tracks

      trackTitle = track.title //replaceAccentuatedLetters(track.title).toLowerCase
      artistFacebookUrlAndTrackTitle = TrackTitleAndArtistFacebookUrl(
        title = trackTitle,
        artistFacebookUrl = artistFacebookUrl)

      if isNotAlreadySaved(track, artistFacebookUrlAndTrackTitle)

    } yield track

    tracksToReturn
  }

  def isNotAlreadySaved(track: Track, trackTitleAndArtistFacebookUrl: TrackTitleAndArtistFacebookUrl): Boolean = {
    val artistFacebookUrl: String = track.artistFacebookUrl

    if (track.url == "1-7g17xJHdI") false

    else if (!artistFacebookUrlToTrackUrl.getIfPresent(artistFacebookUrl).contains(track.url.trim) &&
        !artistFacebookUrlToTrackTitleAndArtistName.getIfPresent(artistFacebookUrl).contains(trackTitleAndArtistFacebookUrl)) {

      keepTracksFilteredInMemory(track, trackTitleAndArtistFacebookUrl, artistFacebookUrl)
    }

    else false
  }

  def keepTracksFilteredInMemory(track: Track, trackTitleAndArtistFacebookUrl: TrackTitleAndArtistFacebookUrl, artistFacebookUrl: String): Boolean = {
    artistFacebookUrlToTrackUrl.put(
      artistFacebookUrl,
      artistFacebookUrlToTrackUrl.getIfPresent(artistFacebookUrl) += track.url.trim)

    artistFacebookUrlToTrackTitleAndArtistName.put(
      artistFacebookUrl,
      artistFacebookUrlToTrackTitleAndArtistName.getIfPresent(artistFacebookUrl) += trackTitleAndArtistFacebookUrl)

    true
  }
}

