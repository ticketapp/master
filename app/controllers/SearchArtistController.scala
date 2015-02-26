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
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  case class Track(url: Option[String],
                   title: Option[String],
                   thumbnail: Option[String],
                   avatarUrl: Option[String],
                   from: String)

  case class FacebookArtist(name: String,
                            id: String,
                            cover: String,
                            websites: Seq[String],
                            link: String)


  def removeLastSlashIfExists(string: String): String  = {
    string.takeRight(1) match {
      case "/" => string.dropRight(1)
      case _ => string
    }
  }

  def websitesStringToWebsitesSeq(websites: Option[String]): Seq[String] = {
    websites match {
      case None => Seq.empty
      case Some(websites: String) =>
        """(https?:\/\/(www\.)?)""".r.replaceAllIn(websites, p => " ").replace("www.", "")
          .split(" ").map { site =>
          removeLastSlashIfExists(site.toLowerCase)
        }
    }
  }

  def findFacebookArtists(pattern: String): Future[Seq[FacebookArtist]] = {
    val readName: Reads[String] = (__ \ "name").read[String]
    val readCategory: Reads[String] = (__ \ "category").read[String]
    val readId: Reads[String] = (__ \ "id").read[String]
    val readCoverSource: Reads[String] = (__ \ "source").read[String]
    val readOptionalCover: Reads[Option[String]] = (__ \ "cover").readNullable(readCoverSource)
    val readWebsites: Reads[Option[String]] = (__ \ "website").readNullable
    val readLink: Reads[String] = (__ \ "link").read[String]
    val readAllArtist: Reads[(String, String, String, Option[String], Option[String], String)] =
      readName.and(readId).and(readCategory).and(readOptionalCover).and(readWebsites).and(readLink)
        .apply((name: String, id: String, category: String, maybeCover: Option[String], website: Option[String],
                link: String) => (name, id, category, maybeCover, website, link))
    val readArtistsArray = Reads.seq(readAllArtist)
    val readArtistsWithCover: Reads[Seq[FacebookArtist]] = readArtistsArray.map { artists =>
      artists.collect{ case (name, id, "Musician/band", Some(cover), websites, link) =>
        FacebookArtist(name, id, cover, websitesStringToWebsitesSeq(websites),
          removeLastSlashIfExists("""(https?:\/\/(www\.)?)""".r.replaceAllIn(link, p => "").toLowerCase))
      }
    }

    WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=400&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
      .map { response =>
      (response.json \ "data").asOpt[Seq[FacebookArtist]](readArtistsWithCover).getOrElse(Seq()).take(20)
    }
  }


  def findSoundCloudTracksForArtist(artist: FacebookArtist): Future[Seq[Track]] = {
    var soundCloudLink: String = ""
    for (site <- artist.websites) {
      site.indexOf("soundcloud.com") match {
        case -1 =>
        case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
      }
    }
    soundCloudLink match {
      case "" => findSoundCloudTracksNotDefinedInFb(artist)
      case scLink => findSoundCloudTracks(scLink)
    }
  }

  def findSoundCloudTracks(scLink: String): Future[Seq[Track]] = {
    val soundCloudTrackReads: Reads[Track] = (
      (__ \ "stream_url").readNullable[String] and
        (__ \ "title").readNullable[String] and
        (__ \ "user" \ "avatar_url").readNullable[String] and
        (__ \ "artwork_url").readNullable[String]
      )((url: Option[String], title: Option[String], avatarUrl: Option[String], thumbnail: Option[String]) =>
        Track(url, title, thumbnail, avatarUrl, "soundcloud"))

    val readTracks: Reads[Seq[Track]] = Reads.seq(soundCloudTrackReads)
    /*val collectOnlyTracksWithUrlAndTitle = readTracks.map { tracks =>
      tracks.collect {
        case (Some(url), Some(title), imageSource) => Track(url)
      }
    }*/
    WS.url("http://api.soundcloud.com/users/" + normalizeString(scLink) + "/tracks?client_id=" +
      soundCloudClientId).get().map { _.json.asOpt[Seq[Track]](readTracks).getOrElse(Seq.empty) }
  }

  def findSoundCloudIds(namePattern: String): Future[Seq[Long]] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    WS.url("http://api.soundcloud.com/users?q=" + normalizeString(namePattern) + "&client_id=" + soundCloudClientId)
      .get().map { _.json.asOpt[Seq[Long]](readSoundCloudIds).getOrElse(Seq.empty) }
  }

  def findSoundCloudWebsites(seqIds: Seq[Long]): Future[Seq[(Long, Seq[String])]] = {
    val readUrls: Reads[Seq[Option[String]]] = Reads.seq((__ \ "url").readNullable)
    Future.sequence(
      seqIds.map { id =>
        WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles?client_id=" + soundCloudClientId).get().map {
          websites => (id, websites.json.asOpt[Seq[Option[String]]](readUrls).getOrElse(Seq.empty).flatten.map {
            website => removeLastSlashIfExists("""(https?:\/\/(www\.)?)""".r.replaceAllIn(website, p => "").toLowerCase)
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
        val site = removeLastSlashIfExists("""(https?:\/\/(www\.)?)""".r.replaceAllIn(website, p => "").toLowerCase )
        if (site == artist.link || artist.websites.indexOf(site) > -1)
          matchedId = websitesAndId._1
      }
    }
    if (matchedId != 0)
      findSoundCloudTracks(matchedId.toString)
    else
      Future{ Seq.empty }
  }

  def findSoundCloudTracksNotDefinedInFb(artist: FacebookArtist): Future[Seq[Track]] = {
    findSoundCloudIds(artist.name).flatMap { ids =>
      findSoundCloudWebsites(ids).flatMap{ websitesAndIds =>
        compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
      }
    }
  }


  def returnSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
    def cleanFacebookId(implicit r: Reads[String]): Reads[String] = r.map(_.substring(16)) //16 = "facebook:artist:".length
    val TupleEnIdFbIdReads = (
      (__ \ "id").read[String] and
        (__ \ "foreign_ids").lazyReadNullable(
          Reads.seq( cleanFacebookId( (__ \ "foreign_id").read[String] ) )
        )
        tupled
      )

    val keepOnlyValidTuples = Reads.seq(TupleEnIdFbIdReads).map { tuples =>
      tuples.collect {
        case (echonestId: String, Some(facebookId: Seq[String])) => (echonestId, facebookId(0))
      }
    }

    WS.url("http://developer.echonest.com/api/v4/artist/search?api_key=" + echonestApiKey + "&name=" +
      normalizeString(artistName) + "&format=json&bucket=urls&bucket=images&bucket=id:facebook" ).get()
      .map { artists =>
      (artists.json \ "response" \ "artists")
        .asOpt(keepOnlyValidTuples)
        .getOrElse(Seq.empty)
    }
  }

  def findEchonestIdCorrespondingToFacebookId(futureSeqIndexEchonestIdAndFacebookId: Future[Seq[(String, String)]],
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

  def findEchonestSongs(echonestId: String): Future[Set[String]] = {
    val titleReads: Reads[Option[String]] = (__ \\ "title").readNullable[String]
    WS.url("http://developer.echonest.com/api/v4/artist/songs?api_key=" + echonestApiKey + "&id=" + echonestId +
      "&format=json&results=50").get().map { songs =>
      (songs.json \ "response" \ "songs").asOpt[Set[Option[String]]](Reads.set(titleReads))
        .getOrElse(Set.empty)
        .flatten
    }
  }

  def findYoutubeVideos(tracksTitle: Set[String], artistName: String):
  Future[Set[Track]] = {
    val youtubeTrackReads: Reads[Track] = (
      (__ \ "id" \ "videoId").read[Option[String]] and
        (__ \ "snippet" \ "title").read[Option[String]] and
        (__ \ "snippet" \ "thumbnails" \ "default" \ "url").readNullable[String]
      )((videoId: Option[String], title: Option[String], thumbnail: Option[String]) =>
        Track(videoId, title, thumbnail, None, "youtube"))

    Future.sequence(
      tracksTitle.map { trackTitle =>
        WS.url("https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
          normalizeString(trackTitle) + normalizeString(artistName) +
          "&type=video&videoCategoryId=10&key=" + youtubeKey
        ).get().map { video =>
          (video.json \ "items").asOpt[Set[Track]](Reads.set(youtubeTrackReads))
            .getOrElse(Seq.empty)
            .filter(_.title.getOrElse("").indexOf(artistName) > -1)
        }
      }
    ).map { _.toSet.flatten }
  }

  def futureYoutubeTracksByEchonestId(artistName: String, echonestId: String, facebookArtistId: String):
  Future[Set[Track]] = {
    findEchonestSongs(echonestId).flatMap { echonestSongsTitle: Set[String] =>
      findYoutubeVideos(echonestSongsTitle, artistName)
    }
  }

  def returnFutureYoutubeTracks(artistName: String, artistId: String, pattern: String): Future[Set[Track]] = {
    findEchonestIdCorrespondingToFacebookId(returnSeqTupleEchonestIdFacebookId(artistName), artistId).flatMap {
      case None => findEchonestIdCorrespondingToFacebookId(returnSeqTupleEchonestIdFacebookId(pattern), artistId)
        .flatMap {
        case Some(echonestId) => futureYoutubeTracksByEchonestId(artistName, echonestId, artistId)
        case None => Future { Set.empty }
      }
      case Some(echonestId) => futureYoutubeTracksByEchonestId(artistName, echonestId, artistId)
    }
  }

  def findFacebookArtistsContaining(pattern: String) = Action.async {
    val sanitizedPattern = normalizeString(pattern.replaceAll(" ", "+"))

    findFacebookArtists(sanitizedPattern).map { facebookArtists =>
      val futureSoundCloudTracks = Future.sequence(
        facebookArtists.map { artist =>
          findSoundCloudTracksForArtist(artist).map { soundCloudTracks =>
            Map( artist.id -> soundCloudTracks )
          }
        }
      )
      val soundCloudTracksEnumerator = Enumerator.flatten(
        futureSoundCloudTracks.map { soundCloudTracks =>
          Enumerator( Json.toJson(soundCloudTracks) )
        }
      )

      val futureYoutubeTracks = Future.sequence(
        facebookArtists.map { artist =>
          returnFutureYoutubeTracks(artist.name, artist.id, sanitizedPattern).map { youtubeTracks =>
            Map( artist.id -> youtubeTracks )
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
