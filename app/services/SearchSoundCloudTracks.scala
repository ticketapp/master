package services

import controllers.DAOException
import controllers.SearchArtistsController._
import models.{Genre, Artist, Track}
import play.api.libs.json._
import play.api.libs.ws.{WS, Response}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import scala.util.matching._
import java.util.regex.Pattern
import models.Genre.saveGenreForArtistInFuture
import Utilities.soundCloudClientId
import java.util.UUID.randomUUID

object SearchSoundCloudTracks {

  def getSoundCloudTracksForArtist(artist: Artist): Future[Seq[Track]] =
    artist.websites find (_ contains "soundcloud.com") match {
      case None =>
        getSoundCloudTracksNotDefinedInFb(artist)
      case Some(soundCloudLink) =>
        getSoundCloudTracksWithLink(
          removeUselessInSoundCloudWebsite(soundCloudLink).substring("soundcloud.com/".length), artist)
    }

  def getSoundCloudTracksNotDefinedInFb(artist: Artist): Future[Seq[Track]] =
    getSoundCloudIdsForName(artist.name) flatMap {
      getSoundcloudWebsites(_) flatMap { listOfTupleIdScAndWebsite =>
        val listOfScWithConfidence = listOfTupleIdScAndWebsite.map { tuple =>
          computationScConfidence(artist, tuple.websites, tuple.soundcloudId)
        }
        Future.sequence(
        listOfScWithConfidence.sortWith(_.confidence > _.confidence)
          .filter(a => a.confidence == listOfScWithConfidence.head.confidence && a.confidence > 0)
          .map { verifiedSC =>
          getSoundCloudTracksWithLink(verifiedSC.soundcloudId.toString, artist)
        }).map { _.flatten }
      }
    }

  case class SoundCloudArtistConfidence(artistId: Option[Long], soundcloudId: Long, confidence: Float)

  def computationScConfidence(artist: Artist, soundCloudWebsites: Seq[String], soundCloudId: Long): SoundCloudArtistConfidence = {
    if(soundCloudWebsites.filter(_ contains "facebook.com/").exists(_ contains artist.facebookUrl) ||
      soundCloudWebsites.filter(_ contains "facebook.com/").exists(_ contains Some(artist.facebookId))) {
      SoundCloudArtistConfidence(artist.artistId, soundCloudId, 1.toFloat)
    } else {
      val SCWebsitesWithoutFacebook = soundCloudWebsites.filterNot(_ contains "facebook.com/")
      val numberScWebsites = SCWebsitesWithoutFacebook.size
      val numberSameWebsites = numberScWebsites - SCWebsitesWithoutFacebook.filterNot(artist.websites).size
      if (numberSameWebsites == 0) {
        SoundCloudArtistConfidence(artist.artistId, soundCloudId, 0)
      } else {
        SoundCloudArtistConfidence(artist.artistId, soundCloudId,calculateConfidence(numberScWebsites, numberSameWebsites))
      }
    }
  }

  def calculateConfidence(numberScWebsites: Int, numberSameWebsites: Int): Float = {
    val up = numberSameWebsites.toDouble
    val down = (numberScWebsites - numberSameWebsites).toDouble
    val n = up + down
    val z = 1.64485
    val phat = up / n
    ((phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)).toFloat
  }

  case class WebsitesForSoundcloudId(soundcloudId: Long, websites: Seq[String])

  def getSoundcloudWebsites(soundcloudIds: Seq[Long]): Future[Seq[WebsitesForSoundcloudId]] = Future.sequence(
    soundcloudIds.map { id =>
      WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles")
        .withQueryString("client_id" -> soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        WebsitesForSoundcloudId(id, readSoundCloudWebsites(soundCloudResponse).map { normalizeUrl })
      }
    }
  )

  def readSoundCloudWebsites(soundCloudResponse: Response): Seq[String] = {
    val readSoundCloudUrls: Reads[Seq[String]] = Reads.seq((__ \ "url").read[String])
    soundCloudResponse.json
      .asOpt[Seq[String]](readSoundCloudUrls)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudIdsForName(namePattern: String): Future[Seq[Long]] = {
    WS.url("http://api.soundcloud.com/users")
      .withQueryString(
        "q" -> namePattern,
        "client_id" -> soundCloudClientId)
      .get()
      .map { readSoundCloudIds }
  }

  def readSoundCloudIds(soundCloudResponse: Response): Seq[Long] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    soundCloudResponse.json
      .asOpt[Seq[Long]](readSoundCloudIds)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudTracksWithLink(soundCloudLink: String, artist: Artist): Future[Seq[Track]] = {
    WS.url("http://api.soundcloud.com/users/" + soundCloudLink + "/tracks")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { response => readSoundCloudTracks(response.json, artist) }
  }

  def readSoundCloudTracks(soundCloudJsonResponse: JsValue, artist: Artist): Seq[Track] = {
    val soundCloudTrackReads = (
      (__ \ "stream_url").readNullable[String] and
        (__ \ "title").readNullable[String] and
        (__ \ "permalink_url").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String] and
        (__ \ "genre").readNullable[String]
      )((url: Option[String], title: Option[String], redirectUrl: Option[String], avatarUrl: Option[String],
         thumbnail: Option[String], genre: Option[String]) =>
      (url, title, redirectUrl, thumbnail, avatarUrl, genre))

    val onlyTracksWithUrlTitleAndThumbnail =
      Reads.seq(soundCloudTrackReads).map { collectOnlyValidTracksAndSaveArtistGenres(_, artist) }

    soundCloudJsonResponse
      .asOpt[Seq[Track]](onlyTracksWithUrlTitleAndThumbnail)
      .getOrElse(Seq.empty)
  }

  def collectOnlyValidTracksAndSaveArtistGenres(tracks: Seq[(Option[String], Option[String], Option[String],
    Option[String], Option[String], Option[String])], artist: Artist): Seq[Track] = {
    tracks.collect {
      case (Some(url), Some(title), redirectUrl: Option[String], Some(thumbnailUrl: String), avatarUrl, genre) =>
        saveGenreForArtistInFuture(genre, artist.artistId.getOrElse(-1L).toInt)
        Track(randomUUID, normalizeTrackTitle(title, artist.name), url, 's', thumbnailUrl, artist.facebookUrl, artist.name,
          redirectUrl)
      case (Some(url), Some(title), redirectUrl: Option[String], None, Some(avatarUrl: String), genre) =>
        saveGenreForArtistInFuture(genre, artist.artistId.getOrElse(-1L).toInt)
        Track(randomUUID, normalizeTrackTitle(title, artist.name), url, 's', avatarUrl, artist.facebookUrl, artist.name,
          redirectUrl)
    }
  }

  def normalizeTrackTitle(title: String, artistName: String): String =
    ("""(?i)""" + Pattern.quote(artistName) + """\s*[:/-]?\s*""").r.replaceFirstIn(
      """(?i)(\.wm[a|v]|\.ogc|\.amr|\.wav|\.flv|\.mov|\.ram|\.mp[3-5]|\.pcm|\.alac|\.eac-3|\.flac|\.vmd)\s*$""".r
        .replaceFirstIn(title, ""),
      "")

  def addSoundCloudWebsitesIfNotInWebsites(maybeTrack: Option[Track], artist: Artist): Future[Seq[String]] =
     maybeTrack match {
    case None => Future(Seq.empty)
    case Some(track: Track) => track.redirectUrl match {
      case None => Future(Seq.empty)
      case Some(redirectUrl) => val normalizedUrl = removeUselessInSoundCloudWebsite(normalizeUrl(redirectUrl)).
        substring("soundcloud.com/".length)
        WS.url("http://api.soundcloud.com/users/" + normalizedUrl + "/web-profiles")
          .withQueryString("client_id" -> soundCloudClientId)
          .get()
          .map { soundCloudResponse =>
          readSoundCloudWebsites(soundCloudResponse).map { website =>
            val normalizedWebsite = normalizeUrl(website)
            if (!artist.websites.contains(normalizedWebsite) && normalizedWebsite.indexOf("facebook") == -1) {
              Artist.addWebsite(artist.artistId, normalizedWebsite)
            }
            }
          readSoundCloudWebsites(soundCloudResponse)
          }
    }
  }
}
