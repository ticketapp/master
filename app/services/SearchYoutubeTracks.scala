package services

import java.util.UUID._

import models.{Artist, Track}
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import play.api.libs.iteratee.Input.EOF
import play.api.libs.ws.{WS, Response}
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import services.Utilities.normalizeUrl
import scala.concurrent.Future
import services.SearchSoundCloudTracks.normalizeTrackTitle
import models.Genre.saveGenreForArtistInFuture
import scala.language.postfixOps
import services.Utilities._

object SearchYoutubeTracks {
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")

  def getYoutubeTracksForArtist(artist: Artist, pattern: String): Enumerator[Set[Track]] = Enumerator.flatten(
    getMaybeEchonestIdByFacebookId(artist) map {
    case Some(echonestId) => getYoutubeTracksByEchonestId(artist, echonestId)
    case None => getYoutubeTracksIfEchonestIdNotFoundByFacebookId(artist, pattern)
    }
  )

  def getYoutubeTracksIfEchonestIdNotFoundByFacebookId(artist: Artist, pattern: String): Enumerator[Set[Track]] = {
    val facebookId = artist.facebookId.get //.get is sure while called by getYoutubeTracksForArtist

    val toBeReturned = getMaybeEchonestArtistUrlsByFacebookId(facebookId).map {
      case Some(idUrls: (String, Set[String])) =>
        val echonestId = idUrls._1
        val echonestWebsites = idUrls._2
        if ((echonestWebsites intersect artist.websites).nonEmpty)
          getYoutubeTracksByEchonestId(artist, echonestId)
        else
          Enumerator.eof[Set[Track]]
      case None => getYoutubeTracksIfNotFoundDirectlyByEchonest(artist, pattern)
    }
    Enumerator.flatten(toBeReturned)
  }

  def getYoutubeTracksIfNotFoundDirectlyByEchonest(artist: Artist, pattern: String): Enumerator[Set[Track]] = {
    val facebookId = artist.facebookId.get //.get is sure while called by getYoutubeTracksIfEchonestIdNotFoundByFacebookId
    val toBeReturned = getEchonestIdCorrespondingToFacebookId(getSeqTupleEchonestIdFacebookId(artist.name), facebookId)
    .map {
      case Some(echonestId) =>
        getYoutubeTracksByEchonestId(artist, echonestId)
      case None =>
        Enumerator.flatten(
          getEchonestIdCorrespondingToFacebookId(getSeqTupleEchonestIdFacebookId(pattern), facebookId)
            .map {
            case Some(echonestId) => getYoutubeTracksByEchonestId(artist, echonestId)
            case None => Enumerator.eof[Set[Track]]
          }
        )
    }
    Enumerator.flatten(toBeReturned)
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

  def getYoutubeTracksByEchonestId(artist: Artist, echonestId: String): Enumerator[Set[Track]] = {
    eventuallySaveArtistGenres(echonestId, artist)

    val enumerateSongsTitle: Enumerator[Set[String]] = getEchonestSongs(echonestId)

    val toEventuallyTracks: Enumeratee[Set[String], Future[Set[Track]]] = Enumeratee.map[Set[String]]{ tracksTitle =>
      getYoutubeTracksByTitlesAndArtistName(artist, tracksTitle)
    }

    enumerateSongsTitle &> toEventuallyTracks &> Enumeratee.mapM(identity)
  }

  def eventuallySaveArtistGenres(echonestId: String, artist: Artist): Unit = Future {
    saveArtistGenres(getArtistGenresOnEchonest(echonestId, artist.artistId
      .getOrElse(throw new Exception("SearchYoutubeTracks.getYoutubeTracksByEchonestId: artist without id found"))))
  }

  def getYoutubeTracksByTitlesAndArtistName(artist: Artist, tracksTitle: Set[String]): Future[Set[Track]] =
    Future.sequence(
      tracksTitle.map { getYoutubeTracksByTitleAndArtistName(artist, _) }
    ).map { nestedTracks => nestedTracks.flatten }

  def getYoutubeTracksByTitleAndArtistName(artist: Artist, trackTitle: String): Future[Seq[Track]] = {
    WS.url("https://www.googleapis.com/youtube/v3/search")
      .withQueryString(
        "part" -> "snippet",
        "q" -> (artist.name + " " + trackTitle),
        "type" -> "video",
        "videoCategoryId" -> "10",
        "key" -> youtubeKey)
      .get()
      .map { readYoutubeTracks(_, artist) }
  }

  def readYoutubeTracks(youtubeResponse: Response, artist: Artist): Seq[Track] = {
    val youtubeTrackReads = (
      (__ \ "snippet" \ "title").readNullable[String] and
        (__ \ "id" \ "videoId").readNullable[String] and
        (__ \ "snippet" \ "thumbnails" \ "default" \ "url").readNullable[String]
      )((title: Option[String], url: Option[String], thumbnailUrl: Option[String]) =>
      (title, url, thumbnailUrl))
    val collectOnlyValidTracks = Reads.seq(youtubeTrackReads) map { tracks =>
      tracks.collect {
        case (Some(title: String), Some(url: String), Some(thumbnailUrl: String))
          if isArtistNameInTrackTitle(title, artist.name) =>
          Track(randomUUID.toString, normalizeTrackTitle(title, artist.name), url, 'y', thumbnailUrl, artist.facebookUrl,
            artist.name)
      }
    }

    (youtubeResponse.json \ "items")
      .asOpt[Seq[Track]](collectOnlyValidTracks)
      .getOrElse(Seq.empty)
  }

  def isArtistNameInTrackTitle(trackTitle: String, artistName: String): Boolean =
    trackTitle.toLowerCase contains artistName.toLowerCase

  def getMaybeEchonestArtistUrlsByFacebookId(facebookArtistId: String): Future[Option[(String, Set[String])]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/urls")
      .withQueryString(
        "api_key" -> echonestApiKey,
        "id" -> s"facebook:artist:$facebookArtistId",
        "format" -> "json")
      .get()
      .map {
      readMaybeEchonestArtistIdAndUrls
    }
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

  def readUrlsFromJsObject(urlsJsObject: JsObject): Set[String] = urlsJsObject.values.map { url =>
    normalizeUrl(url.as[String])
  }.toSet

  def getMaybeEchonestIdByFacebookId(artist: Artist): Future[Option[String]] = artist.facebookId match {
    case None => Future { None }
    case Some(facebookId) =>
    WS.url("http://developer.echonest.com/api/v4/artist/profile")
      .withQueryString(
        "api_key" -> echonestApiKey,
        "id" -> ("facebook:artist:" +facebookId),
        "format" -> "json")
      .get()
      .map { echonestResponse => getEchonestIdIfSameName(echonestResponse.json, artist.name) }
  }

  def getEchonestIdIfSameName(echonestResponse: JsValue, artistName: String): Option[String] = {
    val echonestName = (echonestResponse \ "response" \ "artist" \ "name")
      .asOpt[String]
      .getOrElse("")
      .toLowerCase
    if (echonestName == artistName.toLowerCase)
      (echonestResponse \ "response" \ "artist" \ "id").asOpt[String]
    else
      None
  }

  def getEchonestSongs(echonestArtistId: String): Enumerator[Set[String]] = {
    def getEchonestSongsFrom(start: Long, echonestArtistId: String): Enumerator[Set[String]] = {
      Enumerator.flatten(
        getEchonestSongsOnEchonest(start: Long, echonestArtistId: String).map { echonestResponse =>
          val total = (echonestResponse \ "response" \ "total").asOpt[Int]
          total.exists(_ > start + 100) match {
            case false =>
              Enumerator.eof
            case true =>
              Enumerator(readEchonestSongs(echonestResponse)) >>> getEchonestSongsFrom(start + 100, echonestArtistId)
          }
        }
      )
    }
    getEchonestSongsFrom(0, echonestArtistId)
  }

  def getEchonestSongsOnEchonest(start: Long, echonestArtistId: String): Future[JsValue] = {
    WS.url("http://developer.echonest.com/api/v4/artist/songs")
      .withQueryString(
        "api_key" -> echonestApiKey,
        "id" -> echonestArtistId,
        "format" -> "json",
        "start" -> start.toString,
        "results" -> "100")
      .get()
      .map (_.json)
  }

  def readEchonestSongs(result: JsValue): Set[String] = {
    val titleReads: Reads[Option[String]] = (__ \\ "title").readNullable[String]
    (result \ "response" \ "songs")
      .as[Set[Option[String]]](Reads.set(titleReads))
      .flatten
  }

  def  getSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
    WS.url("http://developer.echonest.com/api/v4/artist/search")
      .withQueryString(
        "name" -> artistName,
        "format" -> "json",
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
          Reads.seq(cleanFacebookId((__ \ "foreign_id").read[String]))
        )
        tupled
      )
    val collectOnlyValidTuples = Reads.seq(TupleEnIdFbIdReads).map { tuples =>
      tuples.collect {
        case (echonestId: String, Some(facebookId: Seq[String])) if facebookId.nonEmpty =>
          (echonestId, facebookId.head)
      }
    }
    (echonestResponse.json \ "response" \ "artists")
      .asOpt[Seq[(String, String)] ](collectOnlyValidTuples)
      .getOrElse(Seq.empty)
  }

  def getArtistGenresOnEchonest(echonestId: String, artistId: Long): Future[(Long, Array[String])] = {
    WS.url("http://developer.echonest.com/api/v4/artist/profile")
      .withQueryString(
        "id" -> echonestId,
        "format" -> "json",
        "bucket" -> "genre",
        "api_key" -> echonestApiKey)
      .get()
      .map { response => (artistId, readEchonestGenres(response.json)) }
  }

  def saveArtistGenres(tupleArtistIdGenres: Future[(Long, Array[String])]): Unit = {
    tupleArtistIdGenres.map { artistIdGenres =>
      artistIdGenres._2.foreach { genre =>
        saveGenreForArtistInFuture(Option(genre), artistIdGenres._1.toInt)
      }
    }
  }

  def readEchonestGenres(echonestJsonResponse: JsValue): Array[String] = {
    val genreReads: Reads[Option[String]] = (__ \\ "name").readNullable[String]
    (echonestJsonResponse \ "response" \ "artist" \ "genres")
      .asOpt[Set[Option[String]]](Reads.set(genreReads))
      .getOrElse(Set.empty)
      .flatten
      .mkString(" ")
      .split(" ")
  }
}
