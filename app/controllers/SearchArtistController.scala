package controllers

import json.JsonHelper._
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import services.Utilities.normalizeString
import play.api.libs.functional.syntax._

object SearchArtistController extends Controller {
  val token = play.Play.application.configuration.getString("facebook.token")
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
  val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
  val echonestBaseUrl = play.Play.application.configuration.getString("echonest.baseUrl")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  case class Track(url: String,
                   title: String,
                   thumbnail: Option[String],
                   avatarUrl: Option[String],
                   platform: String)

  case class FacebookArtist(name: String,
                            id: String,
                            cover: String,
                            websites: Set[String],
                            link: String,
                            description: Option[String],
                            genre: Option[String])


  def removeLastSlashIfExists(string: String): String  = {
    string.takeRight(1) match {
      case "/" => string.dropRight(1)
      case _ => string
    }
  }

  def normalizeUrl(website: String): String = {
    removeLastSlashIfExists("""(https?:\/\/(www\.)?)""".r.replaceAllIn(website, p => "").toLowerCase )
  }

  def websitesStringToWebsitesSet(websites: Option[String]): Set[String] = {
    websites match {
      case None => Set.empty
      case Some(websites: String) =>
        """((https?:\/\/(www\.)?)|www\.)""".r.replaceAllIn(websites, p => " ")
          .split("\\s+").filterNot(_ == "").map { site =>
          removeLastSlashIfExists(site.toLowerCase)
        }.toSet
    }
  }

  def getFacebookArtists(pattern: String): Future[Seq[FacebookArtist]] = {
    val readArtist = (
      (__ \ "name").read[String] and
        (__ \ "category").read[String] and
        (__ \ "id").read[String] and
        (__ \ "cover").readNullable[String](
          (__ \ "source").read[String]
        ) and
        (__ \ "website").readNullable[String] and
        (__ \ "link").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "genre").readNullable[String]
      ).apply((name: String, category: String, id: String, maybeCover: Option[String], website: Option[String],
               link: String,  maybeDescription: Option[String], maybeGenre: Option[String]) =>
      (name, id, category, maybeCover, website, link, maybeDescription, maybeGenre))

    val readArtistsWithCover: Reads[Seq[FacebookArtist]] = Reads.seq(readArtist).map { artists =>
      artists.collect{
        case (name: String, id, "Musician/band", Some(cover: String), websites, link, maybeDescription, maybeGenre) =>
          println(maybeDescription)
          println(maybeGenre)
          FacebookArtist(name, id, cover, websitesStringToWebsitesSet(websites),
            normalizeUrl(link), maybeDescription, maybeGenre )
      }
    }

    WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=400&type=page&fields=name,cover%7Bsource%7D,id,category,link,website,description,genre&access_token=" + token).get()
      .map { response =>
      (response.json \ "data").asOpt[Seq[FacebookArtist]](readArtistsWithCover).getOrElse( Seq.empty ).take(20)
    }
  }

  def getSoundCloudTracksForArtist(artist: FacebookArtist): Future[Seq[Track]] = {
    var soundCloudLink: String = ""
    for (site <- artist.websites) {
      site.indexOf("soundcloud.com") match {
        case -1 =>
        case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
      }
    }
    soundCloudLink match {
      case "" => getSoundCloudTracksNotDefinedInFb(artist)
      case scLink => getSoundCloudTracks(scLink)
    }
  }

  def getSoundCloudTracks(scLink: String): Future[Seq[Track]] = {
    val soundCloudTrackReads = (
      (__ \ "stream_url").readNullable[String] and
        (__ \ "title").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String]
      )((url: Option[String], title: Option[String], avatarUrl: Option[String], thumbnail: Option[String]) =>
        (url, title, thumbnail, avatarUrl))

    val readTracks = Reads.seq(soundCloudTrackReads)
    val collectOnlyTracksWithUrlAndTitle = readTracks.map { tracks =>
      tracks.collect {
        case (Some(url: String), Some(title: String), imageSource, avatarUrl) =>
          Track(url, title, imageSource, avatarUrl, "Soundcloud")
      }
    }
    WS.url("http://api.soundcloud.com/users/" + normalizeString(scLink) + "/tracks?client_id=" +
      soundCloudClientId).get().map { _.json.asOpt[Seq[Track]](collectOnlyTracksWithUrlAndTitle).getOrElse(Seq.empty) }
  }

  def getSoundCloudIds(namePattern: String): Future[Seq[Long]] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    WS.url("http://api.soundcloud.com/users?q=" + normalizeString(namePattern) + "&client_id=" + soundCloudClientId)
      .get().map { _.json.asOpt[Seq[Long]](readSoundCloudIds).getOrElse(Seq.empty) }
  }

  def getSoundCloudWebsites(seqIds: Seq[Long]): Future[Seq[(Long, Seq[String])]] = {
    val readUrls: Reads[Seq[Option[String]]] = Reads.seq((__ \ "url").readNullable)
    Future.sequence(
      seqIds.map { id =>
        WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles?client_id=" + soundCloudClientId).get().map {
          websites => (id, websites.json.asOpt[Seq[Option[String]]](readUrls).getOrElse(Seq.empty).flatten.map {
            website => normalizeUrl(website)
          })
        }
      }
    )
  }

  def compareArtistWebsitesWSCWebsitesAndAddTracks(artist: FacebookArtist, websitesAndIds: Seq[(Long, Seq[String])])
  :Future[Seq[Track]] = {
    var matchedId: Long = 0
    for (websitesAndId <- websitesAndIds) {
      for (website <- websitesAndId._2) {
        val site = normalizeUrl(website)
        if (site == artist.link || artist.websites.toSeq.indexOf(site) > -1)
          matchedId = websitesAndId._1
      }
    }
    if (matchedId != 0)
      getSoundCloudTracks(matchedId.toString)
    else
      Future{ Seq.empty }
  }

  def getSoundCloudTracksNotDefinedInFb(artist: FacebookArtist): Future[Seq[Track]] = {
    getSoundCloudIds(artist.name).flatMap { ids =>
      getSoundCloudWebsites(ids).flatMap{ websitesAndIds =>
        compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
      }
    }
  }

  def returnSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
    def cleanFacebookId(implicit r: Reads[String]): Reads[String] = r.map(_.substring(16)) //16 = "facebook:artist:".length
    val TupleEnIdFbIdReads = (
      (__ \ "id").read[String] and
        (__ \ "foreign_ids").lazyReadNullable(
          Reads.seq( cleanFacebookId((__ \ "foreign_id").read[String]) )
        )
        tupled
      )

    val keepOnlyValidTuples = Reads.seq(TupleEnIdFbIdReads).map { tuples =>
      tuples.collect {
        case (echonestId: String, Some(facebookId: Seq[String])) if facebookId.nonEmpty => (echonestId, facebookId(0))
      }
    }

    WS.url(echonestBaseUrl + "/artist/search?api_key=" + echonestApiKey + "&name=" +
      normalizeString(artistName) + "&format=json&bucket=urls&bucket=images&bucket=id:facebook" ).get()
      .map { artists =>
      (artists.json \ "response" \ "artists")
        .asOpt(keepOnlyValidTuples)
        .getOrElse(Seq.empty)
    }
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
    //println(echonestArtistId)
    val endpoint = s"$echonestBaseUrl/artist/songs"
    val assembledUrl = s"$endpoint?api_key=$echonestApiKey&id=$echonestArtistId&format=json&start=$start&results=100"
    val response = WS.url(assembledUrl).get()
    val futureJson = response map (_.json)

    futureJson flatMap { result =>
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
    WS.url(s"$echonestBaseUrl/artist/profile?api_key=" + echonestApiKey + "&id=facebook:artist:" +
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
    WS.url(s"$echonestBaseUrl/artist/urls?api_key=" + echonestApiKey + "&id=facebook:artist:" +
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

    val collectOnlyTracksWithUrlAndTitle = Reads.set(youtubeTrackReads).map { tracks =>
      tracks.collect {
        case (Some(url: String), Some(title: String), imageSource) =>
          Track(url, title, imageSource, None, "Youtube")
      }
    }

    Future.sequence(
      tracksTitle.map { trackTitle =>
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + normalizeString(trackTitle) +
          normalizeString(artistName) + "&type=video&videoCategoryId=10&key=" + youtubeKey

        WS.url(url).get() map { videos =>
          (videos.json \ "items").asOpt[Set[Track]](collectOnlyTracksWithUrlAndTitle)
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

  def returnFutureYoutubeTracksIfNotFoundDirectlyByEchonest(pattern: String, facebookArtistId: String,
                                                            facebookArtistName: String): Future[Set[Track]] = {
    getEchonestIdCorrespondingToFacebookId(returnSeqTupleEchonestIdFacebookId(facebookArtistName),
      facebookArtistId) flatMap {
      case None => getEchonestIdCorrespondingToFacebookId(
        returnSeqTupleEchonestIdFacebookId(pattern), facebookArtistId) flatMap {
        case Some(echonestId) => futureYoutubeTracksByEchonestId(facebookArtistName, echonestId)
        case None => Future { Set.empty }
      }
      case Some(echonestId) =>futureYoutubeTracksByEchonestId(facebookArtistName, echonestId)
    }
  }

  def returnFutureYoutubeTracks(facebookArtist: FacebookArtist, pattern: String): Future[Set[Track]] = {
   isEchonestArtistFoundByFacebookArtistId(facebookArtist.id, facebookArtist.name) flatMap {
     case Some(echonestArtistId: String) =>
       futureYoutubeTracksByEchonestId(facebookArtist.name, echonestArtistId)
     case None =>
       getEchonestArtistUrls(facebookArtist.id) flatMap { idUrls =>
       idUrls._1 match {
         case Some(echonestId) =>
           if ((idUrls._2 intersect facebookArtist.websites).nonEmpty)
             futureYoutubeTracksByEchonestId(facebookArtist.name, echonestId)
           else
             returnFutureYoutubeTracksIfNotFoundDirectlyByEchonest(pattern, facebookArtist.id, facebookArtist.name)
         case None =>
           returnFutureYoutubeTracksIfNotFoundDirectlyByEchonest(pattern, facebookArtist.id, facebookArtist.name)
       }
     }
   }
  }

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    val sanitizedPattern = normalizeString(pattern.replaceAll(" ", "+"))

    getFacebookArtists(sanitizedPattern).map { facebookArtists =>
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
    }
  }
}
