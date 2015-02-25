package controllers

import json.JsonHelper._
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.annotation.tailrec
import scala.concurrent.Future
import services.Utilities.normalizeString
import play.api.libs.functional.syntax._

object SearchArtistController extends Controller {
    val token = play.Play.application.configuration.getString("facebook.token")
    val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
    val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
    val youtubeKey = play.Play.application.configuration.getString("youtube.key")
    case class SoundCloudTrack(stream_url: Option[String], title: Option[String], artwork_url: Option[String] )
    case class YoutubeTrack(videoId: String, title: String, thumbnail: Option[String] )
    case class FacebookArtist(name: String,
                              id: String,
                              cover: String,
                              websites: Seq[String],
                              link: String,
                              soundCloudTracks: Seq[SoundCloudTrack] = Seq(),
                              youtubeTracks: Set[YoutubeTrack] = Set() )


    def removeLastSlashIfExists(string: String): String  = {
      string.takeRight(1) match {
        case "/" => string.dropRight(1)
        case _ => string
      }
    }

    def websitesStringToWebsitesSeq(websites: Option[String]): Seq[String] = {
      var seqOfWebSitesToReturn: Seq[String] = Seq()
      websites match {
        case None => Seq()
        case Some(websitesFound) =>
          //.split seq (donc to array) et ensuite toseq??? surement pas top cette fonction...
          for (website <- websitesFound.split(" ").toSeq) {
            seqOfWebSitesToReturn = seqOfWebSitesToReturn ++
              """(https?:\/\/(www\.)?)""".r.replaceAllIn(website, p => " ").replace("www.", "").split(" ").toSeq
                .filter(_ != "").map { site =>
                removeLastSlashIfExists(site.toLowerCase)
              }
          }
          seqOfWebSitesToReturn
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

    def findSoundCloudUserImageIfNoTrackImage(soundCloudLink: String, soundCloudTracks: Seq[SoundCloudTrack]):
    Future[Seq[SoundCloudTrack]] = {
      @tailrec
      def isThereASoundCloudTrackWithoutImage(soundCloudTracks: List[SoundCloudTrack]): Boolean = {
        soundCloudTracks match {
          case x :: tail if x.artwork_url == None => true
          case Nil => false
          case x :: tail  => isThereASoundCloudTrackWithoutImage(tail)
        }
      }
      if (isThereASoundCloudTrackWithoutImage(soundCloudTracks.toList)) {
        val readUrl: Reads[String] = (__ \ "avatar_url").read[String]
        WS.url("http://api.soundcloud.com/users/" + normalizeString(soundCloudLink) + "?client_id=" +
          soundCloudClientId).get().map { user =>
          user.json.asOpt[String](readUrl) match {
            case None => soundCloudTracks
            case Some(imgUrl) =>
              soundCloudTracks.map { soundCloudTrack =>
                if (soundCloudTrack.artwork_url == None) soundCloudTrack.copy(artwork_url = Some(imgUrl))
                else soundCloudTrack
              }
          }
        }
      } else {
        Future { soundCloudTracks }
      }
    }

    def findSoundCloudTracksForArtist(artist: FacebookArtist): Future[Seq[SoundCloudTrack]] = {
      var soundCloudLink: String = ""
      for (site <- artist.websites) {
        site.indexOf("soundcloud.com") match {
          case -1 =>
          case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
        }
      }
      soundCloudLink match {
        case "" => Future { artist.soundCloudTracks }
        case scLink => findSoundCloudTracks(scLink)
      }
    }

    def findSoundCloudTracks(scLink: String): Future[Seq[SoundCloudTrack]] = {
      val readTracks: Reads[Seq[SoundCloudTrack]] = Reads.seq(soundCloudTracksReads)
      /*val collectOnlyTracksWithUrlAndTitle = readTracks.map { tracks =>
        tracks.collect {
          case (Some(url), Some(title), imageSource) => SoundCloudTrack(url)
        }
      }*/
      WS.url("http://api.soundcloud.com/users/" + normalizeString(scLink) + "/tracks?client_id=" +
        soundCloudClientId).get().flatMap { soundCloudTracks =>
        findSoundCloudUserImageIfNoTrackImage(scLink,
          soundCloudTracks.json.asOpt[Seq[SoundCloudTrack]](readTracks).getOrElse(Seq()) )
      }
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
    :Future[Seq[SoundCloudTrack]] = {
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
        Future{ artist.soundCloudTracks }
    }

    def findSoundCloudTracksNotDefinedInFb(artist: FacebookArtist): Future[Seq[SoundCloudTrack]] = {
      artist.soundCloudTracks match {
        case tracks: Seq[soundCloudTracks] if tracks.isEmpty =>
          findSoundCloudIds(artist.name).flatMap { ids =>
            findSoundCloudWebsites(ids).flatMap{ websitesAndIds =>
              compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
            }
          }
        case _ => Future{ artist.soundCloudTracks }
      }
    }

    def returnFutureArtistsWSoundCloudTracks(pattern: String): Future[Seq[Future[FacebookArtist]]] = {
      findFacebookArtists(pattern).map { facebookArtists: Seq[FacebookArtist] =>
        facebookArtists.map { facebookArtist =>
          findSoundCloudTracksForArtist(facebookArtist).map { soundCloudTracks =>
            facebookArtist.copy(soundCloudTracks = soundCloudTracks)
          }
        }
      }
    }

    def returnSeqTupleEchonestIdFacebookId(artistName: String): Future[Seq[(String, String)]] = {
      def cleanFacebookId(implicit r: Reads[String]): Reads[String] = r.map(_.substring(17)) //17 = "facebook:artist:".length
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
        toBeReturned //si toBeReturned is empty => onre fait pareil avec la seqe des ids de la recherche avec pattern au lieu de artist.name
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

    def findYoutubeVideos(tracksTitle: Set[String], artistName: String): Future[Set[YoutubeTrack]] = {
      implicit val youtubeTrackReads: Reads[YoutubeTrack] = (
        (JsPath \ "id" \ "videoId").read[String] and
          (JsPath \ "snippet" \ "title").read[String] and
          (JsPath \ "snippet" \ "thumbnails" \ "default" \ "url").readNullable[String]
        )(YoutubeTrack.apply _)

      Future.sequence(
        tracksTitle.map { trackTitle =>
          WS.url("https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
            normalizeString(trackTitle) + normalizeString(artistName) +
            "&type=video&videoCategoryId=10&key=" + youtubeKey ).get().map { video =>
            (video.json \ "items").asOpt[Set[YoutubeTrack]](Reads.set(youtubeTrackReads))
              .getOrElse(Seq.empty)
              .filter(_.title.indexOf(artistName) > -1)
          }
        }
      ).map { _.toSet.flatten }
    }

    def futureYoutubeTracksByEchonestId(artistName: String, echonestId: String): Future[Set[YoutubeTrack]] = {
      findEchonestSongs(echonestId).flatMap { echonestSongsTitle: Set[String] =>
        findYoutubeVideos(echonestSongsTitle, artistName)
      }
    }

    def returnFutureYoutubeTracks(artistName: String, artistId: String, pattern: String): Future[Set[YoutubeTrack]] = {
      findEchonestIdCorrespondingToFacebookId(returnSeqTupleEchonestIdFacebookId(artistName), artistId).flatMap {
        case None => findEchonestIdCorrespondingToFacebookId(returnSeqTupleEchonestIdFacebookId(pattern), artistId)
          .flatMap {
          case Some(echonestId) => futureYoutubeTracksByEchonestId(artistName, echonestId)
          case None => Future { Set.empty }
        }
        case Some(echonestId) => futureYoutubeTracksByEchonestId(artistName, echonestId)
      }
    }


  def mock(serviceName: String) = {
    val start = System.currentTimeMillis()
    def getLatency(r: Any): Long = System.currentTimeMillis() - start
    val token = play.Play.application.configuration.getString("facebook.token")
    val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
    val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
    val youtubeKey = play.Play.application.configuration.getString("youtube.key")
    serviceName match {
      case "a" =>
        WS.url("http://api.soundcloud.com/users/rone-music?client_id=" +
          soundCloudClientId).get().map { response => Json.toJson("yo")}
      case "b" =>
        WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
          + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
          .map { response => Json.toJson("sacoche!!!")}
      case _ => WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
        + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
        .map { response => Json.toJson("???!!!")}
    }
  }

  def findFacebookArtistsContaining(pattern: String) = Action.async {
    /*
    returnFutureArtistsWSoundCloudTracks(sanitizedPattern).map { seqFutureArtistWSoundCloudTracks: Seq[Future[FacebookArtist]] =>
      val futureSeqArtistWSoundCloudTracks: Future[Seq[FacebookArtist]] = Future.sequence(seqFutureArtistWSoundCloudTracks)
      val artistWMoreSoundCloudTracks: Future[Seq[Future[FacebookArtist]]] =
        futureSeqArtistWSoundCloudTracks.map { seqArtist: Seq[FacebookArtist] =>
          seqArtist.map { findSoundCloudTracksNotDefinedInFb }
      }

      for {

      } yield {

      }
    }*/
    val sanitizedPattern = normalizeString(pattern.replaceAll(" ", "+"))
    /*
    returnFutureArtistsWSoundCloudTracks(sanitizedPattern).flatMap { resp =>
      Future.sequence(resp).map { a =>
        val b = Enumerator(Json.toJson(Seq("lkjlkj", "kljlkjlkj", "kljlkjljlj", "kljlkjljk")))
        val c = Enumerator.flatten(mock("a").map { str => Enumerator(Json.toJson(Map("champ" -> str))) })
        val d = Enumerator(Json.toJson(a))

        val e = Enumerator.interleave(b, c, d)

        Ok.chunked(e)
        //Ok(Json.toJson(a))
      }
    }*/
    returnFutureArtistsWSoundCloudTracks(sanitizedPattern).flatMap { seqFutureArtist: Seq[Future[FacebookArtist]] =>
      val futureSeqArtist = Future.sequence(seqFutureArtist): Future[Seq[FacebookArtist]]
      futureSeqArtist.flatMap { seqArtist: Seq[FacebookArtist] =>

        val seqFutureArtistWMoreSCTracks: Seq[Future[FacebookArtist]] = seqArtist.map { artist: FacebookArtist =>
          findSoundCloudTracksNotDefinedInFb(artist).map { soundCloudTracksNotDefinedInFb =>
            artist.copy(soundCloudTracks = soundCloudTracksNotDefinedInFb)
          }
        }
        val futureSeqArtistWMoreSCTracks: Future[Seq[FacebookArtist]] = Future.sequence(seqFutureArtistWMoreSCTracks)

        futureSeqArtistWMoreSCTracks.map { seqArtist =>
          seqArtist.map { artist: FacebookArtist =>
            returnFutureYoutubeTracks(artist.name, artist.id, sanitizedPattern).map { youtubeTracks =>
              artist.copy(youtubeTracks = youtubeTracks)
            }
          }
        }.flatMap { seqOfFuture: Seq[Future[FacebookArtist]] =>
          Future.sequence(seqOfFuture).map { seq =>
            Enumerator(seq)
            Ok(Json.toJson(seq))
          }
        }
      }
    }
  }
}