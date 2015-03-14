package services

import controllers.DAOException
import models.{Artist, Track}
import play.api.libs.json._
import play.api.libs.ws.{WS, Response}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import scala.util.matching._
import java.util.regex.Pattern

object SearchSoundCloudTracks {
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")

  def getSoundCloudTracksForArtist(artist: Artist): Future[Seq[Track]] = {
    // regex indexOf directly list
    var soundCloudLink: String = ""
    for (site <- artist.websites) {
      site.indexOf("soundcloud.com") match {
        case -1 =>
        case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
      }
    }
    soundCloudLink match {
      case "" => getSoundCloudTracksNotDefinedInFb(artist)
      case scLink => getSoundCloudTracksWithLink(scLink, artist)
    }
  }

  def getSoundCloudTracksNotDefinedInFb(artist: Artist): Future[Seq[Track]] = {
    getSoundCloudIdsForName(artist.name).flatMap { ids =>
      getTupleIdAndSoundCloudWebsitesForIds(ids).flatMap { websitesAndIds =>
        compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
      }
    }
  }

  def compareArtistWebsitesWSCWebsitesAndAddTracks(artist: Artist, websitesAndIds: Seq[(Long, Seq[String])])
  :Future[Seq[Track]] = {
    var matchedId: Long = 0
    for (websitesAndId <- websitesAndIds) {
      for (website <- websitesAndId._2) {
        val site = normalizeUrl(website)
        if (artist.websites.toSeq.indexOf(site) > -1 || ("facebook.com/" + artist.facebookUrl) == site ||
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
          .withQueryString("client_id" -> soundCloudClientId)
          .get()
          .map { soundCloudResponse => (id, readSoundCloudWebsites(soundCloudResponse).map { normalizeUrl })
        }
      }
    )
  }

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

  def getSoundCloudTracksWithLink(scLink: String, artist: Artist): Future[Seq[Track]] = {
    WS.url("http://api.soundcloud.com/users/" + scLink + "/tracks")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { response => readSoundCloudTracks(response.json, artist) }
  }

  def readSoundCloudTracks(soundCloudResponse: JsValue, artist: Artist): Seq[Track] = {
    println(soundCloudResponse)
    val soundCloudTrackReads = (
      (__ \ "stream_url").read[String] and
        (__ \ "title").read[String] and
        (__ \ "permalink_url").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String]
      )((url: String, title: String, redirectUrl: Option[String], avatarUrl: Option[String],
         thumbnail: Option[String]) => (url, title, redirectUrl, thumbnail, avatarUrl))
    val readTracks = Reads.seq(soundCloudTrackReads)
    val collectOnlyTracksWithUrlTitleAndThumbnail = readTracks.map { tracks =>
      tracks.collect {
        case (url, title, redirectUrl: Option[String], Some(thumbnailUrl: String), avatarUrl) =>
          Track(-1L, normalizeTrackTitle(title, artist.name), url, "Soundcloud", thumbnailUrl, artist.facebookUrl,
            redirectUrl)
        case (url, title, redirectUrl: Option[String], None, Some(avatarUrl: String)) =>
          Track(-1L, normalizeTrackTitle(title, artist.name), url, "Soundcloud", avatarUrl, artist.facebookUrl,
            redirectUrl)
      }
    }
    soundCloudResponse
      .asOpt[Seq[Track]](collectOnlyTracksWithUrlTitleAndThumbnail)
      .getOrElse(Seq.empty)
  }

  def normalizeTrackTitle(title: String, artistName: String): String = {
    //+ artistName_ (ou : / etc)
    val a = ("""(?i)""" + Pattern.quote(artistName) + """\s*-?\s*""").r.replaceFirstIn(
      """(?i)(\.wm[a|v]|\.ogc|\.amr|\.wav|\.flv|\.mov|\.ram|\.mp[3-5]|\.pcm|\.alac|\.eac-3|\.flac|\.vmd)\s*$""".r.
        replaceFirstIn(title, ""),
      "")
    println("a: " + a)
    title
    a
  }
}
