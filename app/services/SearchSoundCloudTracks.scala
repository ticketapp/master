package services

import java.util.UUID.randomUUID
import java.util.regex.Pattern
import javax.inject.Inject

import models.{ArtistMethods, GenreMethods, Artist, Track}
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Future

class SearchSoundCloudTracks @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                        val utilities: Utilities,
                                        val artistMethods: ArtistMethods,
                                        val genreMethods: GenreMethods) {

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
     getTupleIdAndSoundCloudWebsitesForIds(_) flatMap { compareArtistWebsitesWSCWebsitesAndGetTracks(artist, _) }
   }

  def compareArtistWebsitesWSCWebsitesAndGetTracks(artist: Artist, idAndWebsitesSeq: Seq[(Long, Seq[String])])
  :Future[Seq[Track]] = {
    var matchedId: Long = 0
    for (websitesAndId <- idAndWebsitesSeq) {
      for (website <- websitesAndId._2) {
        val site = utilities.normalizeUrl(website)
        if (artist.websites.toSeq.contains(site) || ("facebook.com/" + artist.facebookUrl) == site ||
          (artist.facebookId.nonEmpty && (site contains artist.facebookId.get)))
          matchedId = websitesAndId._1
      }
    }
    if (matchedId != 0)
      getSoundCloudTracksWithLink(matchedId.toString, artist)
    else
      Future { Seq.empty }
  }

  def getTupleIdAndSoundCloudWebsitesForIds(ids: Seq[Long]): Future[Seq[(Long, Seq[String])]] = {
    Future.sequence(
      ids.map { id =>
        WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles")
          .withQueryString("client_id" -> utilities.soundCloudClientId)
          .get()
          .map { soundCloudWSResponse => (id, readSoundCloudWebsites(soundCloudWSResponse).map { utilities.normalizeUrl })
        }
      }
    )
  }

  def readSoundCloudWebsites(soundCloudWSResponse: WSResponse): Seq[String] = {
    val readSoundCloudUrls: Reads[Seq[String]] = Reads.seq((__ \ "url").read[String])
    soundCloudWSResponse.json
      .asOpt[Seq[String]](readSoundCloudUrls)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudIdsForName(namePattern: String): Future[Seq[Long]] = {
    WS.url("http://api.soundcloud.com/users")
      .withQueryString(
        "q" -> namePattern,
        "client_id" -> utilities.soundCloudClientId)
      .get()
      .map { readSoundCloudIds }
  }

  def readSoundCloudIds(soundCloudWSResponse: WSResponse): Seq[Long] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    soundCloudWSResponse.json
      .asOpt[Seq[Long]](readSoundCloudIds)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudTracksWithLink(soundCloudLink: String, artist: Artist): Future[Seq[Track]] = {
    WS.url("http://api.soundcloud.com/users/" + soundCloudLink + "/tracks")
      .withQueryString("client_id" -> utilities.soundCloudClientId)
      .get()
      .map { response => readSoundCloudTracks(response.json, artist) }
  }

  def readSoundCloudTracks(soundCloudJsonWSResponse: JsValue, artist: Artist): Seq[Track] = {
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

    soundCloudJsonWSResponse
      .asOpt[Seq[Track]](onlyTracksWithUrlTitleAndThumbnail)
      .getOrElse(Seq.empty)
  }

  def collectOnlyValidTracksAndSaveArtistGenres(tracks: Seq[(Option[String], Option[String], Option[String],
    Option[String], Option[String], Option[String])], artist: Artist): Seq[Track] = {
    tracks.collect {
      case (Some(url), Some(title), redirectUrl: Option[String], Some(thumbnailUrl: String), avatarUrl, genre) =>
        genreMethods.saveGenreForArtistInFuture(genre, artist.id.getOrElse(-1L))
        Track(randomUUID, normalizeTrackTitle(title, artist.name), url, 's', thumbnailUrl, artist.facebookUrl, artist.name,
          redirectUrl)
      case (Some(url), Some(title), redirectUrl: Option[String], None, Some(avatarUrl: String), genre) =>
        genreMethods.saveGenreForArtistInFuture(genre, artist.id.getOrElse(-1L))
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
      case Some(redirectUrl) => val normalizedUrl = utilities.normalizeUrl(redirectUrl)
        if (!artist.websites.contains(
          utilities.normalizeUrl(normalizedUrl).dropRight(normalizedUrl.length - normalizedUrl.lastIndexOf("/")))) {
          artistMethods.addWebsite(artist.id.get, normalizedUrl)
        }
    }
  }

  def removeUselessInSoundCloudWebsite(website: String): String = website match {
    case soundCloudWebsite if soundCloudWebsite contains "soundcloud" =>
      if (soundCloudWebsite.count(_ == '/') > 1)
        soundCloudWebsite.take(soundCloudWebsite.lastIndexOf('/'))
      else
        soundCloudWebsite
    case _ => website
  }
}
