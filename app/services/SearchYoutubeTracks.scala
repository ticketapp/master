package services

import models.{Artist, Track}
import play.api.libs.ws.{WS, Response}
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import scala.concurrent.Future
import java.util.regex.Pattern
import services.SearchSoundCloudTracks.normalizeTrackTitle

object SearchYoutubeTracks {
  val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")

  def getYoutubeTracksForArtist(artist: Artist, pattern: String): Future[Set[Track]] = {
    getMaybeEchonestIdByFacebookId(artist) flatMap {
      case Some(echonestId) => getYoutubeTracksByEchonestId(artist, echonestId)
      case None => getYoutubeTracksIfEchonestIdNotFoundByFacebookId(artist, pattern)
    }
  }

  def getYoutubeTracksIfEchonestIdNotFoundByFacebookId(artist: Artist, pattern: String): Future[Set[Track]] = {
    val facebookId = artist.facebookId.get //.get is sure while called by getYoutubeTracksForArtist
    getMaybeEchonestArtistUrlsByFacebookId(facebookId) flatMap {
      case Some(idUrls: (String, Set[String])) =>
        val echonestId = idUrls._1
        val echonestWebsites = idUrls._2
        if ((echonestWebsites intersect artist.websites).nonEmpty)
          getYoutubeTracksByEchonestId(artist, echonestId)
        else
          Future { Set.empty }
      case None => getYoutubeTracksIfNotFoundDirectlyByEchonest(artist, pattern)
    }
  }

  def getYoutubeTracksIfNotFoundDirectlyByEchonest(artist: Artist, pattern: String): Future[Set[Track]] = {
    val facebookId = artist.facebookId.get //.get is sure while called by getYoutubeTracksIfEchonestIdNotFoundByFacebookId
    getEchonestIdCorrespondingToFacebookId(
      getSeqTupleEchonestIdFacebookId(artist.name), facebookId
    ) flatMap {
      case Some(echonestId) => getYoutubeTracksByEchonestId(artist, echonestId)
      case None => getEchonestIdCorrespondingToFacebookId(
        getSeqTupleEchonestIdFacebookId(pattern), facebookId
      ) flatMap {
        case Some(echonestId) => getYoutubeTracksByEchonestId(artist, echonestId)
        case None => Future { Set.empty }
      }
    }
  }

  def getEchonestIdCorrespondingToFacebookId(eventuallyTuplesEchonestIdFacebookId: Future[Seq[(String, String)]],
                                             artistId: String): Future[Option[String]] = {
    eventuallyTuplesEchonestIdFacebookId.map { seqTuplesEchonestIdFacebookId =>
      var toBeReturned: Option[String] = None
      for (tuple <- seqTuplesEchonestIdFacebookId) {
        if (tuple._2 == artistId)
          toBeReturned = Some(tuple._1)
      }
      toBeReturned
    }
  }

  def getYoutubeTracksByEchonestId(artist: Artist, echonestId: String): Future[Set[Track]] = {
    getEchonestSongs(0, echonestId).flatMap { echonestSongsTitle: Set[String] =>
      getYoutubeTracksByTitlesAndArtistName(artist, echonestSongsTitle)
    }
  }

  def getYoutubeTracksByTitlesAndArtistName(artist: Artist, tracksTitle: Set[String]): Future[Set[Track]] =
    Future.sequence(tracksTitle.map { getYoutubeTracksByTitleAndArtistName(artist, _) })
      .map { _.flatten }

  def getYoutubeTracksByTitleAndArtistName(artist: Artist, trackTitle: String): Future[Seq[Track]] = {
    WS.url("https://www.googleapis.com/youtube/v3/search")
      .withQueryString(
        "part" -> "snippet",
        "q" -> (trackTitle + artist.name),
        "type" -> "video",
        "videoCategoryId" -> "10",
        "key" -> youtubeKey)
      .get()
      .map { readYoutubeTracks(_, artist) }
  }

  def readYoutubeTracks(youtubeResponse: Response, artist: Artist): Seq[Track] = {
    println("youtubeResponse = " + youtubeResponse.json)
    val youtubeTrackReads = (
      (__ \ "snippet" \ "title").read[String] and
        (__ \ "id" \ "videoId").read[String] and
        (__ \ "snippet" \ "thumbnails" \ "default" \ "url").read[String]
      )((title: String, url: String, thumbnailUrl: String) => (title, url, thumbnailUrl))
    val collectOnlyTracksWithUrlTitleAndThumbnailUrl = Reads.seq(youtubeTrackReads).map { tracks =>
      tracks.collect {
        case (title, url, thumbnailUrl) if isArtistNameInTrackTitle(title, artist.name) =>
          Track(-1L, normalizeTrackTitle(title, artist.name), url, "Youtube", thumbnailUrl, artist.facebookUrl)
      }
    }
    (youtubeResponse.json \ "items")
      .asOpt[Seq[Track]](collectOnlyTracksWithUrlTitleAndThumbnailUrl)
      .getOrElse(Seq.empty)
  }

  def isArtistNameInTrackTitle(trackTitle: String, artistName: String): Boolean = {
  //val ArtistNameRegex = artist.name.toLowerCase.r
    trackTitle.toLowerCase contains artistName.toLowerCase
    /*match {
      case ArtistNameRegex(title) => true
      case _ => false
    }*/
  }

  def getMaybeEchonestArtistUrlsByFacebookId(facebookArtistId: String): Future[Option[(String, Set[String])]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/urls")
      .withQueryString(
        "api_key" -> echonestApiKey,
        "id" -> s"facebook:artist:$facebookArtistId",
        "format" -> "json")
      .get()
      .map { readMaybeEchonestArtistIdAndUrls }
  }

  def readMaybeEchonestArtistIdAndUrls(echonestResponse: Response): Option[(String, Set[String])] = {
    val id = (echonestResponse.json \ "response" \ "id").asOpt[String]
    val urlsJsValue = echonestResponse.json \ "response" \ "urls"
    if (id == None)
      None
    else {
      urlsJsValue match {
        case urlsJsObject: JsObject => Option((id.get, readUrlsFromJsObject(urlsJsObject)))
        case _ => None
      }
    }
  }

  def readUrlsFromJsObject(urlsJsObject: JsObject): Set[String] =
    urlsJsObject.values.map { url =>
      normalizeUrl(url.as[String])
    }.toSet

  def getMaybeEchonestIdByFacebookId(artist: Artist): Future[Option[String]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/profile")
      .withQueryString(
        "api_key" -> echonestApiKey,
        "id" -> ("facebook:artist:" + artist.facebookId),
        "format" -> "json")
      .get()
      .map { getEchonestIdIfEqualNames(_, artist.name) }
  }

  def getEchonestIdIfEqualNames(echonestResponse: Response, artistName: String): Option[String] = {
    val echonestName = (echonestResponse.json \ "response" \ "artist" \ "name")
      .asOpt[String]
      .getOrElse("")
      .toLowerCase
    if (echonestName == artistName.toLowerCase) {
      (echonestResponse.json \ "response" \ "artist" \ "id").asOpt[String]
    }
    else
      None
  }

  def getEchonestSongs(start: Long, echonestArtistId: String): Future[Set[String]] = {
    //faire une inner foncion pour start à 0
    WS.url("http://developer.echonest.com/api/v4/artist/songs")
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
        val songs = readEchonestSongs(result)
        total exists (_ > start + 100) match {
          case false => Future.successful(songs)
          case true => getEchonestSongs(start + 100, echonestArtistId) map (songs ++ _)
        }
      }
  }

  def readEchonestSongs(result: JsValue): Set[String] = {
    val titleReads: Reads[Option[String]] = (__ \\ "title").readNullable[String]
    (result \ "response" \ "songs")
      .asOpt[Set[Option[String]]](Reads.set(titleReads))
      .getOrElse(Set.empty)
      .flatten
  }

  def getSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/search")
      .withQueryString(
        "name" -> artistName,
        "format" -> "json",
        "bucket" -> "urls",
        "bucket" -> "images",
        "bucket" -> "id:facebook",
        "api_key" -> echonestApiKey)
      .get()
      .map { readEchonestTupleIdFacebookId }
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
}
