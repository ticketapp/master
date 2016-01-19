package tracksDomain

import java.util.UUID.randomUUID
import javax.inject.Inject

import artistsDomain.Artist
import genresDomain.GenreMethods
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.Utilities

import scala.concurrent.Future
import scala.util.control.NonFatal

case class SoundCloudArtistConfidence(artistId: Option[Long], soundcloudId: Long, confidence: Double)
case class WebsitesForSoundcloudId(soundcloudId: Long, websites: Seq[String])

class SearchSoundCloudTracks @Inject()(val trackMethods: TrackMethods,
                                       val genreMethods: GenreMethods)
    extends SoundCloudHelper with Utilities {

  def getSoundCloudTracksForArtist(artist: Artist): Future[Seq[Track]] =
    artist.websites find (_ contains "soundcloud.com") match {
      case None =>
        getSoundCloudTracksWithoutSoundCloudWebsite(artist)
      case Some(soundCloudLink) =>
        getSoundCloudTracksWithSoundCloudLink(
          removeUselessInSoundCloudWebsite(soundCloudLink).substring("soundcloud.com/".length), artist)
    }

  def getSoundCloudTracksWithoutSoundCloudWebsite(artist: Artist): Future[Seq[Track]] = getSoundCloudIdsForName(artist.name) flatMap {
    getSoundcloudWebsites(_) flatMap { listOfTupleIdScAndWebsite =>
      val listOfScWithConfidence = listOfTupleIdScAndWebsite.map(tuple => 
        computeSoundCloudConfidence(artist, tuple.websites, tuple.soundcloudId))

      val soundCloudsWithBestConfidence: Seq[SoundCloudArtistConfidence] =
        returnSoundCloudAccountsWithBestConfidence(listOfScWithConfidence)

      Future.sequence(
        soundCloudsWithBestConfidence.map(verifiedSC =>
          getSoundCloudTracksWithSoundCloudLink(verifiedSC.soundcloudId.toString, artist))
      ).map { _.flatten }
    }
  }

  def returnSoundCloudAccountsWithBestConfidence(listOfScWithConfidence: Seq[SoundCloudArtistConfidence])
  : Seq[SoundCloudArtistConfidence] = listOfScWithConfidence match {
    case nonEmptyList if nonEmptyList.nonEmpty =>
      val maxConfidence = listOfScWithConfidence.maxBy(_.confidence).confidence
      val soundCloudsWithBestConfidence = maxConfidence match {
        case 0.0 => Seq.empty
        case max => listOfScWithConfidence.filter(_.confidence == max)
      }
      soundCloudsWithBestConfidence
    case _ =>
      Seq.empty
  }

  def computeSoundCloudConfidence(artist: Artist, soundCloudWebsites: Seq[String], soundCloudId: Long): SoundCloudArtistConfidence = {
    if(soundCloudWebsites.filter(_ contains "facebook.com/").exists(_ contains artist.facebookUrl) ||
      soundCloudWebsites.filter(_ contains "facebook.com/").exists(_ contains Some(artist.facebookId))) {
      SoundCloudArtistConfidence(artist.id, soundCloudId, 1)
    } else {
      val SCWebsitesWithoutFacebook = soundCloudWebsites.filterNot(_ contains "facebook.com/")
      val numberScWebsites = SCWebsitesWithoutFacebook.size
      val numberSameWebsites = numberScWebsites - SCWebsitesWithoutFacebook.filterNot(artist.websites).size
      if (numberSameWebsites == 0) {
        SoundCloudArtistConfidence(artist.id, soundCloudId, 0)
      } else {
        SoundCloudArtistConfidence(artist.id, soundCloudId, calculateConfidence(numberScWebsites, numberSameWebsites))
      }
    }
  }

  def calculateConfidence(numberScWebsites: Int, numberSameWebsites: Int): Double = {
    val up = numberSameWebsites.toDouble
    val down = (numberScWebsites - numberSameWebsites).toDouble
    val n = up + down
    val z = 1.64485
    val phat = up / n
    (phat + z * z / (2 * n) - z * math.sqrt((phat * (1 - phat) + z * z / (4 * n)) / n)) / (1 + z * z / n)
  }

  def getSoundcloudWebsites(soundcloudIds: Seq[Long]): Future[Seq[WebsitesForSoundcloudId]] = Future.sequence(
    soundcloudIds.map { id =>
      WS.url("https://api.soundcloud.com/users/" + id + "/web-profiles")
        .withQueryString("client_id" -> soundCloudClientId)
        .get()
        .map { soundCloudResponse =>
        WebsitesForSoundcloudId(id, readSoundCloudWebsites(soundCloudResponse).map { normalizeUrl })
      }
    }
  )

  def getSoundCloudIdsForName(namePattern: String): Future[Seq[Long]] = WS.
    url("https://api.soundcloud.com/users")
    .withQueryString(
      "q" -> namePattern,
      "client_id" -> soundCloudClientId)
    .get()
    .map{response =>
      Logger.info("SearchSouncloudTracks.getSoundCloudIdsForName.response: " + Json.stringify(response.json))
      readSoundCloudIds(response)
    }
    .recover {
      case NonFatal(e) =>
        Logger.error("SearchSoundCloudTracks.getSoundCLoudIdsForName: " + namePattern, e)
        Seq.empty
    }

  def readSoundCloudIds(soundCloudWSResponse: WSResponse): Seq[Long] = {
    Logger.info("SearchSouncloudTracks.readSoundCloudIds: " + Json.stringify(soundCloudWSResponse.json))
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    soundCloudWSResponse.json
      .asOpt[Seq[Long]](readSoundCloudIds)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudTracksWithSoundCloudLink(soundCloudLink: String, artist: Artist): Future[Seq[Track]] = WS
    .url("https://api.soundcloud.com/users/" + soundCloudLink + "/tracks")
    .withQueryString("client_id" -> soundCloudClientId)
    .get()
    .map{ response =>
      Logger.info("getSoundCloudTracksWithSoundCloudLink.response" + Json.stringify(response.json))
      readSoundCloudTracks(response.json, artist)
    } recover {
      case NonFatal(e) =>
        Logger.error("SearchSoundcloudTracks.getSoundCloudTracksWithSoundCloudLink: for: " + soundCloudLink + "\nMessage:\n", e)
        Seq.empty
    }

  def readSoundCloudTracks(soundCloudJsonWSResponse: JsValue, artist: Artist): Seq[Track] = {
    Logger.info("readSouncloudTracks:" + Json.stringify(soundCloudJsonWSResponse))
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
        Future(genreMethods.saveMaybeGenreOfArtist(genre, artist.id.getOrElse(-1L)))
        Track(randomUUID, trackMethods.normalizeTrackTitle(title, artist.name), url, 's', thumbnailUrl, artist.facebookUrl, artist.name,
          redirectUrl)
      case (Some(url), Some(title), redirectUrl: Option[String], None, Some(avatarUrl: String), genre) =>
        Future(genreMethods.saveMaybeGenreOfArtist(genre, artist.id.getOrElse(-1L)))
        Track(randomUUID, trackMethods.normalizeTrackTitle(title, artist.name), url, 's', avatarUrl, artist.facebookUrl, artist.name,
          redirectUrl)
    }
  }
}
