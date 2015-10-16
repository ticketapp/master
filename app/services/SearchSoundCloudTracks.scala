package services

import java.util.UUID.randomUUID
import java.util.regex.Pattern
import javax.inject.Inject
import services.MyPostgresDriver.api._

import models._
import play.api.Play.current
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Future

class SearchSoundCloudTracks @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                        val utilities: Utilities,
                                        val trackMethods: TrackMethods,
                                        val genreMethods: GenreMethods)
    extends HasDatabaseConfigProvider[MyPostgresDriver] with SoundCloudHelper {

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
      SoundCloudArtistConfidence(artist.id, soundCloudId, 1.toFloat)
    } else {
      val SCWebsitesWithoutFacebook = soundCloudWebsites.filterNot(_ contains "facebook.com/")
      val numberScWebsites = SCWebsitesWithoutFacebook.size
      val numberSameWebsites = numberScWebsites - SCWebsitesWithoutFacebook.filterNot(artist.websites).size
      if (numberSameWebsites == 0) {
        SoundCloudArtistConfidence(artist.id, soundCloudId, 0)
      } else {
        SoundCloudArtistConfidence(artist.id, soundCloudId,calculateConfidence(numberScWebsites, numberSameWebsites))
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
        .withQueryString("client_id" -> utilities.soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        WebsitesForSoundcloudId(id, readSoundCloudWebsites(soundCloudResponse).map { utilities.normalizeUrl })
      }
    }
  )

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
        Track(randomUUID, trackMethods.normalizeTrackTitle(title, artist.name), url, 's', thumbnailUrl, artist.facebookUrl, artist.name,
          redirectUrl)
      case (Some(url), Some(title), redirectUrl: Option[String], None, Some(avatarUrl: String), genre) =>
        genreMethods.saveGenreForArtistInFuture(genre, artist.id.getOrElse(-1L))
        Track(randomUUID, trackMethods.normalizeTrackTitle(title, artist.name), url, 's', avatarUrl, artist.facebookUrl, artist.name,
          redirectUrl)
    }
  }
}
