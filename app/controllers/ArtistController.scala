package controllers

import json.JsonHelper._
import models.Artist
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.annotation.tailrec
import scala.concurrent.Future
import services.Utilities.normalizeString
import play.api.libs.functional.syntax._

object ArtistController extends Controller with securesocial.core.SecureSocial {
  def artists = Action {
    Ok(Json.toJson(Artist.findAll()))
  }

  def artist(artistId: Long) = Action {
    Ok(Json.toJson(Artist.find(artistId)))
  }

  def findArtistsContaining(pattern: String) = Action {
    Ok(Json.toJson(Artist.findAllContaining(pattern)))
  }


  val artistBindingForm = Form( mapping(
      "facebookId" -> optional(nonEmptyText(2)),
      "artistName" -> nonEmptyText(2)
    )(Artist.formApply)(Artist.formUnapply)
  )

  def createArtist = Action { implicit request =>
    try {
      artistBindingForm.bindFromRequest().fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        artist => {
          Ok(Json.toJson(Artist.save(artist)))
        }
      )
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def deleteArtist(artistId: Long) = Action {
    Artist.deleteArtist(artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  def followArtist(userId : Long, artistId : Long) = Action {
    Artist.followArtist(userId, artistId)
    Redirect(routes.Admin.indexAdmin())
  }

  val token = play.Play.application.configuration.getString("facebook.token")
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
  val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
  val youtubeKey = play.Play.application.configuration.getString("youtube.key")
  case class SoundCloudTrack(stream_url: String, title: String, artwork_url: Option[String] )
  case class YoutubeTrack(videoId: String, title: String, thumbnail: Option[String] )
  case class FacebookArtist(name: String,
                            id: String,
                            cover: String,
                            websites: List[String],
                            link: String,
                            soundCloudTracks: Option[List[SoundCloudTrack]] = None,
                            youtubeTracks: Seq[YoutubeTrack] = List() )

  def websitesStringToWebsitesList(websites: Option[String]): List[String] = {
    var listOfWebSitesToReturn: List[String] = List()
    websites match {
      case None => List()
      case Some(websitesFound) =>
        for (website <- websitesFound.split(" ").toList) {
          listOfWebSitesToReturn = listOfWebSitesToReturn :::
            """(https?:\/\/(www\.)?)""".r.replaceAllIn(website, p => " ").replace("www.", "").split(" ").toList
              .filter(_ != "")
        }
        listOfWebSitesToReturn
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
    val collectOnlyMusiciansWithCover: Reads[Seq[(String, String, String, Option[String], String)]] = readArtistsArray
      .map { pages =>
      pages.collect {
        case (name, id, "Musician/band", Some(cover), websites, link) => (name, id, cover, websites, link)
      }
    }
    val readArtists: Reads[Seq[FacebookArtist]] = collectOnlyMusiciansWithCover.map { artists =>
      artists.map{ case (name, id, cover, websites, link) =>
        FacebookArtist(name, id, cover, websitesStringToWebsitesList(websites), link)
      }
    }
    WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=400&type=page&fields=name,cover%7Bsource%7D,id,category,likes,link,website&access_token=" + token).get()
      .map { response =>
      (response.json \ "data").as[Seq[FacebookArtist]](readArtists)
    }
  }

  def findSoundCloudUserImageIfNoTrackImage(soundCloudLink: String, soundCloudTracks: List[SoundCloudTrack]):
  Future[List[SoundCloudTrack]] = {
    @tailrec
    def isThereASoundCloudTrackWithoutImage(soundCloudTracks: List[SoundCloudTrack]): Boolean = {
      soundCloudTracks match {
        case x :: tail if x.artwork_url == None => true
        case Nil => false
        case x :: tail  => isThereASoundCloudTrackWithoutImage(tail)
      }
    }
    if (isThereASoundCloudTrackWithoutImage(soundCloudTracks)) {
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

  def findSoundCloudTracks(artist: FacebookArtist): Future[FacebookArtist] = {
    //rajouter une fonction qui va chercher l'image de l'user si pas d'image pour la track
    val readTracks: Reads[List[SoundCloudTrack]] = Reads.list(soundCloudTracksReads)
    var soundCloudLink: String = ""
    for (site <- artist.websites) {
      site.indexOf("soundcloud.com") match {
        case -1 =>
        case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
      }
    }

    soundCloudLink match {
      case "" => Future { artist }
      case scLink => WS.url("http://api.soundcloud.com/users/" + normalizeString(scLink) + "/tracks?client_id=" +
        soundCloudClientId).get().flatMap { soundCloudTracks =>
          findSoundCloudUserImageIfNoTrackImage(scLink, soundCloudTracks.json.asOpt[List[SoundCloudTrack]](readTracks)
            .getOrElse(List()))
            .map { soundCloudTracksWMoreImages: List[SoundCloudTrack] =>
              artist.copy(soundCloudTracks = Some(soundCloudTracksWMoreImages))
            }
      }
    }
  }

  def findSoundCloudIds(namePattern: String): Future[Seq[Long]] = {
    val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])
    WS.url("http://api.soundcloud.com/users?q=" + normalizeString(namePattern) + "&client_id=" + soundCloudClientId)
      .get().map { users =>
      users.json.as[Seq[Long]](readSoundCloudIds)
    }
  }

  def findSoundCloudWebsites(listIds: Seq[Long]): Future[Seq[(Long, Seq[String])]] = {
    val readUrls: Reads[Seq[Option[String]]] = Reads.seq((__ \ "url").readNullable)
    Future.sequence(
      listIds.map { id =>
        WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles?client_id=" + soundCloudClientId).get().map {
          websites => (id, websites.json.as[Seq[Option[String]]](readUrls).flatten)
        }
      }
    )
  }

  def compareArtistWebsitesWSCWebsitesAndAddTracks(artist: FacebookArtist, websitesAndIds: Seq[(Long, Seq[String])])
  :Future[FacebookArtist] = {
    //refaire une fonction soundcloudtracks search parce que redondance là
    val readTracks: Reads[List[SoundCloudTrack]] = Reads.list(soundCloudTracksReads)
    var matchedId: Long = 0
    for (websitesAndId <- websitesAndIds) {
      for (website <- websitesAndId._2) {
        val site = """(https?:\/\/(www\.)?)""".r.replaceAllIn(website.replaceAll(" ", ""), p => "")
        if (site == artist.link || artist.websites.indexOf(site) > -1) { matchedId = websitesAndId._1 }
      }
    }

    if (matchedId != 0) {
      WS.url("http://api.soundcloud.com/users/" + matchedId + "/tracks?client_id=" + soundCloudClientId)
        .get().map { soundCloudTracks =>
        artist.copy(soundCloudTracks = soundCloudTracks.json.asOpt[List[SoundCloudTrack]](readTracks))
      }
    } else {
      Future{ artist }
    }
  }


  def findSoundCloudTracksNotDefinedInFb(artist: FacebookArtist): Future[FacebookArtist] = {
    artist.soundCloudTracks match {
      case Some(tracks: List[soundCloudTracks]) if tracks.isEmpty =>
        findSoundCloudIds(artist.name).flatMap { ids =>
          findSoundCloudWebsites(ids).flatMap{ websitesAndIds =>
            compareArtistWebsitesWSCWebsitesAndAddTracks(artist, websitesAndIds)
          }
        }
      case _ => Future{ artist }
    }
  }

  def returnFutureArtistsWSoundCloudTracks(pattern: String): Future[Seq[Future[FacebookArtist]]] = {
    findFacebookArtists(pattern).map { facebookArtists: Seq[FacebookArtist] =>
      facebookArtists.map { facebookArtist =>
        findSoundCloudTracks(facebookArtist)
      }
    }
  }


  def findEchonestArtistIds(artist: FacebookArtist): Future[List[(String, String)]] = { //=> (ARDDJUP12B3B35514F, List("facebook:artist:174132699276436"))
    WS.url("http://developer.echonest.com/api/v4/artist/search?api_key=" + echonestApiKey + "&name=" +
      normalizeString(artist.name) + "&format=json&bucket=urls&bucket=images&bucket=id:facebook" ).get()
      .map { artists =>
      val artistsJson = artists.json \ "response" \ "artists"
      val facebookIds = artistsJson.asOpt[Seq[Option[String]]](Reads.seq((__ \\ "foreign_id").readNullable))
      var listIndexFacebookIds: List[Int] = List()
      for ((maybeFbId, index) <- facebookIds.getOrElse(List()).view.zipWithIndex) {
        maybeFbId match {
          case Some(id) => listIndexFacebookIds = index +: listIndexFacebookIds
          case None =>
        }
      }

      listIndexFacebookIds.map { index: Int =>
        ((artistsJson(index) \ "id").as[String], (artistsJson(index) \\ "foreign_id")(0).toString()
          .replace("facebook:artist:", "") )
      }
    }
  }

  def findEchonestIdCorrespondingToFacebookId(futureListIndexEchonestIdAndFacebookId: Future[List[(String, String)]],
                                              artistId: String): Future[Option[String]] = {

    futureListIndexEchonestIdAndFacebookId.map { listIndexEchonestIdAndFacebookId =>
      var toBeReturned: Option[String] = None
      for (tuple <- listIndexEchonestIdAndFacebookId) {
        if (tuple._2.replaceAll("\"", "") == artistId)
          toBeReturned = Some(tuple._1)
      }
      toBeReturned
    }
  }

  def findEchonestSongs(echonestId: String): Future[List[String]] = {
    val titleReads: Reads[Option[String]] = (__ \\ "title").readNullable[String]
    WS.url("http://developer.echonest.com/api/v4/artist/songs?api_key=" + echonestApiKey + "&id=" + echonestId +
      "&format=json&results=50").get().map { songs =>
      (songs.json \ "response" \ "songs").as[List[Option[String]]](Reads.list(titleReads)).flatten.distinct
    }
  }

  def findYoutubeVideos(tracksTitle: Seq[String], artistName: String): Future[Seq[YoutubeTrack]] = {
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
          (video.json \ "items").as[Seq[YoutubeTrack]](Reads.seq(youtubeTrackReads))
        }
      }
    ).map { seqOfSeqYoutubeTracks: Seq[Seq[YoutubeTrack]] =>
      seqOfSeqYoutubeTracks.flatten
    }
  }

  def returnFutureArtistWYoutubeTracks(artist: FacebookArtist): Future[FacebookArtist] = {
    findEchonestIdCorrespondingToFacebookId(findEchonestArtistIds(artist), artist.id).flatMap {
      case None => Future { artist }
      case Some(echonestId) => findEchonestSongs(echonestId).flatMap { echonestSongsTitle: Seq[String] =>
        findYoutubeVideos(echonestSongsTitle, artist.name).map{ youtubeTracks =>
          artist.copy(youtubeTracks = youtubeTracks)
        }
      }
    }
  }

  def findFacebookArtistsContaining(pattern: String) = Action.async {
    returnFutureArtistsWSoundCloudTracks(pattern).flatMap { seqFutureArtist: Seq[Future[FacebookArtist]] =>
      val futureSeqArtist = Future.sequence(seqFutureArtist): Future[Seq[FacebookArtist]]
      futureSeqArtist.flatMap { seqArtist: Seq[FacebookArtist] =>

        val seqFutureArtistWMoreSCTracks: Seq[Future[FacebookArtist]] = seqArtist.map { artist: FacebookArtist =>
          findSoundCloudTracksNotDefinedInFb(artist)
        }
        val futureSeqArtistWMoreSCTracks: Future[Seq[FacebookArtist]] = Future.sequence(seqFutureArtistWMoreSCTracks)

        futureSeqArtistWMoreSCTracks.map { seqArtist =>
          seqArtist.map { artist: FacebookArtist =>
            returnFutureArtistWYoutubeTracks(artist)
          }
        }.flatMap { seqOfFuture =>
          val futureOfSeq: Future[Seq[FacebookArtist]] = Future.sequence(seqOfFuture)
          futureOfSeq.map { seq =>
            Ok(Json.toJson(seq))
          }
        }
      }
    }
  }
}
