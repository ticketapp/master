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
      getTupleIdAndSoundCloudWebsitesForIds(_) flatMap {listOfTupleIdScAndWebsite =>
        val listOfScWithConfidence = listOfTupleIdScAndWebsite.map { tuple =>
          computationScConfidence(artist.websites, tuple._2, artist.facebookUrl, artist.facebookId, tuple._1)
        }
        Future.sequence(
        listOfScWithConfidence.filter(_._2 == listOfScWithConfidence.head._2).sortWith(_._2 > _._2).map { verifiedSC =>
          getSoundCloudTracksWithLink(verifiedSC._1.toString, artist)
        }).map { _.flatten}
      }
    }

  def computationScConfidence(artistWebsites: Set[String], SCWebsites: Seq[String], facebookArtistUrl: String,
                         facebookArtistId: Option[String], ScId: Long): (Long, Float) = {
    if(SCWebsites.filter(_ contains "facebook.com/").exists(_ contains facebookArtistUrl) ||
      SCWebsites.filter(_ contains "facebook.com/").exists(_ contains Some(facebookArtistId))) {
      (ScId, 1.toFloat)
    } else {
      val SCWebsitesWithoutFacebook =  SCWebsites.filterNot(_ contains "facebook.com/")
      val numberScWebsites = SCWebsitesWithoutFacebook.size
      val numberSameWebsites = numberScWebsites - SCWebsitesWithoutFacebook.filterNot(artistWebsites).size
      if (numberSameWebsites == 0) {
        (ScId, 0)
      } else {
        val up = numberSameWebsites.toDouble
        val down = (numberScWebsites - numberSameWebsites).toDouble
        val n = up + down
        val z = 1.64485
        val phat = up / n
        (ScId, ((phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)).toFloat)
      }
    }
  }
  

  def getTupleIdAndSoundCloudWebsitesForIds(ids: Seq[Long]): Future[Seq[(Long, Seq[String])]] = Future.sequence(
    ids.map { id =>
      WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles")
        .withQueryString("client_id" -> soundCloudClientId)
        .get()
        .map { soundCloudResponse => (id, readSoundCloudWebsites(soundCloudResponse).map { normalizeUrl })
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

  def addSoundCloudWebsiteIfNotInWebsites(maybeTrack: Option[Track], artist: Artist): Unit = maybeTrack match {
    case None =>
    case Some(track: Track) => track.redirectUrl match {
      case None =>
      case Some(redirectUrl) => val normalizedUrl = normalizeUrl(redirectUrl)
        if (!artist.websites.contains(
          normalizeUrl(normalizedUrl).dropRight(normalizedUrl.length - normalizedUrl.lastIndexOf("/")))) {
          Artist.addWebsite(artist.artistId, normalizedUrl)
        }
    }
  }
}
