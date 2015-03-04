package controllers

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc.Controller
import scala.concurrent.Future
import models.{Artist, Track}
import json.JsonHelper._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities.{ normalizeUrl, normalizeString }

object SearchTrackController extends Controller {
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
  val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")

  def readSoundCloudTracks(soundCloudResponse: Response): Seq[Track] = {
    val soundCloudTrackReads = (
      (__ \ "stream_url").readNullable[String] and
        (__ \ "title").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String]
      )((url: Option[String], title: Option[String], avatarUrl: Option[String], thumbnail: Option[String]) =>
      (url, title, thumbnail, avatarUrl))

    val readTracks = Reads.seq(soundCloudTrackReads)

    val collectOnlyTracksWithUrlTitleAndThumbnail = readTracks.map { tracks =>
      tracks.collect {
        case (Some(url: String), Some(title: String), Some(thumbnailUrl: String), avatarUrl) =>
          Track(-1L, title, url, "Soundcloud", thumbnailUrl)
        case (Some(url: String), Some(title: String), None, Some(avatarUrl: String)) =>
          Track(-1L, title, url, "Soundcloud", avatarUrl)
      }
    }
    soundCloudResponse.json
      .asOpt[Seq[Track]](collectOnlyTracksWithUrlTitleAndThumbnail)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudTracksWithLink(scLink: String): Future[Seq[Track]] = {
    WS.url("http://api.soundcloud.com/users/" + normalizeString(scLink) + "/tracks")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { readSoundCloudTracks }
  }

  def readSoundCloudIds(soundCloudResponse: Response): Seq[Long] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    soundCloudResponse.json
      .asOpt[Seq[Long]](readSoundCloudIds)
      .getOrElse(Seq.empty)
  }

  def getSoundCloudIdsForName(namePattern: String): Future[Seq[Long]] = {
    WS.url("http://api.soundcloud.com/users")
      .withQueryString(
        "q" -> normalizeString(namePattern),
        "client_id" -> soundCloudClientId)
      .get()
      .map { readSoundCloudIds }
  }

  def readSoundCloudWebsites(soundCloudResponse: Response): Seq[String] = {
    val readSoundCloudUrls: Reads[Seq[String]] = Reads.seq((__ \ "url").read[String])
    soundCloudResponse.json
      .asOpt[Seq[String]](readSoundCloudUrls)
      .getOrElse(Seq.empty)
  }

  //Should be improved
  def getTupleIdAndSoundCloudWebsitesForIds(ids: Seq[Long]): Future[Seq[(Long, Seq[String])]] = {
    Future.sequence(
      ids.map { id =>
        WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles")
          .withQueryString("client_id" -> soundCloudClientId)
          .get()
          .map { soundCloudResponse =>
            (id, readSoundCloudWebsites(soundCloudResponse).map { normalizeUrl })
        }
      }
    )
  }

  def compareArtistWebsitesWSCWebsitesAndAddTracks(artist: Artist, websitesAndIds: Seq[(Long, Seq[String])])
  :Future[Seq[Track]] = {
    var matchedId: Long = 0
    for (websitesAndId <- websitesAndIds) {
      for (website <- websitesAndId._2) {
        val site = normalizeUrl(website)
        if (artist.websites.toSeq.indexOf(site) > -1)
          matchedId = websitesAndId._1
      }
    }
    if (matchedId != 0)
      getSoundCloudTracksWithLink(matchedId.toString)
    else
      Future { Seq.empty }
  }

  def getSoundCloudTracksNotDefinedInFb(artist: Artist): Future[Seq[Track]] = {
    getSoundCloudIdsForName(artist.name).flatMap { ids =>
      getTupleIdAndSoundCloudWebsitesForIds(ids).flatMap{ websitesAndIds =>
        compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
      }
    }
  }

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
      case scLink => getSoundCloudTracksWithLink(scLink)
    }
  }

  def readEchonestTupleIdFacebookId(echonestResponse: Response): Seq[(String, String)] = {
    //16 = "facebook:artist:".length
    def cleanFacebookId(implicit r: Reads[String]): Reads[String] = r.map(_.substring(16))
    val TupleEnIdFbIdReads = (
        (__ \ "id").read[String] and
          (__ \ "foreign_ids").lazyReadNullable(
            Reads.seq( cleanFacebookId((__ \ "foreign_id").read[String]) )
          )
          tupled
        )

    val collectOnlyValidTuples = Reads.seq(TupleEnIdFbIdReads).map { tuples =>
      tuples.collect {
        case (echonestId: String, Some(facebookId: Seq[String])) if facebookId.nonEmpty =>
          (echonestId, facebookId(0))
      }
    }

    (echonestResponse.json \ "response" \ "artists")
      .asOpt[Seq[(String, String)] ](collectOnlyValidTuples)
      .getOrElse(Seq.empty)
  }

  def getSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/search")
      .withQueryString(
      "name=" -> normalizeString(artistName),
      "format=" -> "json",
      "bucket=" -> "urls",
      "bucket=" -> "images",
      "bucket=" -> "id:facebook",
      "api_key=" -> echonestApiKey)
      .get()
      .map { readEchonestTupleIdFacebookId }
  }

  def getEchonestIdCorrespondingToFacebookId(futureSeqIndexEchonestIdAndFacebookId: Future[Seq[(String, String)]],
                                             artistId: String): Future[Option[String]] = {
    futureSeqIndexEchonestIdAndFacebookId.map { seqIndexEchonestIdAndFacebookId =>
      var toBeReturned: Option[String] = None
      for (tuple <- seqIndexEchonestIdAndFacebookId) {
        if (tuple._2 == artistId)
          toBeReturned = Some(tuple._1)
      }
      toBeReturned
    }
  }

  def getEchonestSongs(start: Long, echonestArtistId: String): Future[Set[JsValue]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/search/artist/songs")
      .withQueryString(
    "api_key" -> echonestApiKey,
    "id" -> echonestArtistId,
    "format" -> "json",
    "start" -> start.toString,
    "results" -> "100")
      .get()
      .map (_.json)
      .flatMap { result =>
      val total = (result \ "response" \ "total").asOpt[Int]
      val songs = (result \ "response" \ "songs").as[Set[JsValue]]
      Future.successful(songs)
      /*total exists (_ > start + 100) match {
        case false => Future.successful(songs)
        case true => getEchonestSongs(start + 100, echonestArtistId) map (songs ++ _)
      }*/
    }
  }

  def isEchonestArtistFoundByFacebookArtistId(facebookArtistId: String, artistName: String): Future[Option[String]] = {
    WS.url(s"http://developer.echonest.com/api/v4/artist/search/artist/profile?api_key=" + echonestApiKey + "&id=facebook:artist:" +
      facebookArtistId + "&format=json").get().map { profile =>
      if ( (profile.json \ "response" \ "artist" \ "name").asOpt[String].getOrElse("").toLowerCase ==
        artistName.toLowerCase) {
        (profile.json \ "response" \ "artist" \ "id").asOpt[String]
      }
      else
        None
    }
  }


  def getEchonestArtistUrls(facebookArtistId: String): Future[(Option[String], Set[String])] = {
    WS.url(s"http://developer.echonest.com/api/v4/artist/search/artist/urls?api_key=" + echonestApiKey + "&id=facebook:artist:" +
      facebookArtistId + "&format=json").get() map { urls =>
      urls.json \ "response" \ "urls" match {
        case json: JsObject => ((urls.json \ "response" \ "id").asOpt[String],
          json.values.map { url => normalizeUrl(url.as[String]) }.toSet)
        case _ => (None, Set(""))
      }
    }
  }

  def getYoutubeVideos(tracksTitle: Set[String], artistName: String): Future[Set[Track]] = {
    val youtubeTrackReads = (
      (__ \ "id" \ "videoId").read[Option[String]] and
        (__ \ "snippet" \ "title").read[Option[String]] and
        (__ \ "snippet" \ "thumbnails" \ "default" \ "url").readNullable[String]
      )((videoId: Option[String], title: Option[String], thumbnail: Option[String]) =>
      (videoId, title, thumbnail))

    val collectOnlyTracksWithUrlTitleAndImage = Reads.set(youtubeTrackReads).map { tracks =>
      tracks.collect {
        case (Some(url: String), Some(title: String), Some(imageSource: String)) =>
          Track(-1L, url, title, imageSource, "Youtube")
      }
    }

    Future.sequence(
      tracksTitle.map { trackTitle =>
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + normalizeString(trackTitle) +
          normalizeString(artistName) + "&type=video&videoCategoryId=10&key=" + youtubeKey

        WS.url(url).get() map { videos =>
          (videos.json \ "items").asOpt[Set[Track]](collectOnlyTracksWithUrlTitleAndImage)
            .getOrElse(Set.empty)
            .filter(_.title.toLowerCase.indexOf(artistName.toLowerCase) > -1)
        }
      }
    ).map { _.toSet.flatten }
  }

  def futureYoutubeTracksByEchonestId(artistName: String, echonestId: String): Future[Set[Track]] = {
    getEchonestSongs(0, echonestId).map(_.map(_ \ "title").map(_.as[String]))
      .flatMap { echonestSongsTitle: Set[String] =>
      getYoutubeVideos(echonestSongsTitle, artistName)
    }
  }

  def getYoutubeTracksIfNotFoundDirectlyByEchonest(pattern: String, facebookArtistId: String,
                                                   facebookArtistName: String): Future[Set[Track]] = {
    getEchonestIdCorrespondingToFacebookId(getSeqTupleEchonestIdFacebookId(facebookArtistName),
      facebookArtistId) flatMap {
      case None => getEchonestIdCorrespondingToFacebookId(
        getSeqTupleEchonestIdFacebookId(pattern), facebookArtistId) flatMap {
        case Some(echonestId) => futureYoutubeTracksByEchonestId(facebookArtistName, echonestId)
        case None => Future { Set.empty }
      }
      case Some(echonestId) =>futureYoutubeTracksByEchonestId(facebookArtistName, echonestId)
    }
  }

  def returnFutureYoutubeTracks(facebookArtist: Artist, pattern: String): Future[Set[Track]] = {
    isEchonestArtistFoundByFacebookArtistId(facebookArtist.facebookId.get, facebookArtist.name) flatMap {
      case Some(echonestArtistId: String) =>
        futureYoutubeTracksByEchonestId(facebookArtist.name, echonestArtistId)
      case None =>
        getEchonestArtistUrls(facebookArtist.facebookId.get) flatMap { idUrls =>
          idUrls._1 match {
            case Some(echonestId) =>
              if ((idUrls._2 intersect facebookArtist.websites).nonEmpty)
                futureYoutubeTracksByEchonestId(facebookArtist.name, echonestId)
              else
                getYoutubeTracksIfNotFoundDirectlyByEchonest(pattern, facebookArtist.facebookId.get, facebookArtist.name)
            case None =>
              getYoutubeTracksIfNotFoundDirectlyByEchonest(pattern, facebookArtist.facebookId.get, facebookArtist.name)
          }
        }
    }
  }


  /*
  .map { facebookArtists =>
      val futureSoundCloudTracks = Future.sequence(
        facebookArtists.map { facebookArtist =>
          getSoundCloudTracksForArtist(facebookArtist).map { soundCloudTracks =>
            Map( facebookArtist.id -> soundCloudTracks )
          }
        }
      )
      val soundCloudTracksEnumerator = Enumerator.flatten(
        futureSoundCloudTracks.map { soundCloudTracks =>
          Enumerator( Json.toJson(soundCloudTracks) )
        }
      )

      val futureYoutubeTracks = Future.sequence(
        facebookArtists.map { facebookArtist =>
          returnFutureYoutubeTracks(facebookArtist, sanitizedPattern).map { youtubeTracks =>
            Map( facebookArtist.id -> youtubeTracks )
          }
        }
      )
      val youtubeTracksEnumerator = Enumerator.flatten(
        futureYoutubeTracks.map { youtubeTracks =>
          Enumerator( Json.toJson(youtubeTracks) )
        }
      )

      val enumerators = Enumerator.interleave(
        Enumerator( Json.toJson(facebookArtists) ), soundCloudTracksEnumerator, youtubeTracksEnumerator
      )
      Ok.chunked(enumerators)
   */
}
