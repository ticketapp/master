package controllers

import json.JsonHelper._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import jobs.Scheduler.formatDescription
import models.Artist
import models.Image
import models.Genre
import play.api.libs.json.Reads._
import services.Utilities.{ normalizeUrl, getNormalizedWebsitesInText }

object SearchArtistsController extends Controller {
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
  val token = play.Play.application.configuration.getString("facebook.token")
  val linkPattern = play.Play.application.configuration.getString("regex.linkPattern").r

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    getEventuallyFacebookArtists(pattern).map { artists =>
      Ok(Json.toJson(artists))
    }
  }

  def getEventuallyFacebookArtists(pattern: String): Future[Seq[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/search")
      .withQueryString(
        "q" -> pattern,
        "type" -> "page",
        "limit" -> "400",
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> token)
      .get()
      .map { readFacebookArtists } //(_).take(20)
  }
  
  def getEventuallyArtistsInEventTitle(artistsNameInTitle: Seq[String], webSites: Set[String]): Future[Seq[Artist]] = {
    Future.sequence(
      artistsNameInTitle.map {
        getEventuallyFacebookArtists(_).map { artists => artists }
      }
    ).map { _.flatten collect { case artist: Artist if (artist.websites intersect webSites).nonEmpty => artist } }
  }
  
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
    ).apply((name: String, category: String, id: String, maybeCover: Option[String], websites: Option[String],
             link: String, maybeDescription: Option[String], maybeGenre: Option[String]) =>
    (name, id, category, maybeCover, websites, link, maybeDescription, maybeGenre))

  def readFacebookArtists(facebookResponse: Response): Seq[Artist] = {
    val collectOnlyArtistsWithCover: Reads[Seq[Artist]] = Reads.seq(readArtist).map { artists =>
      artists.collect {
        case (name, facebookId, "Musician/band", Some(cover: String), websites, link, maybeDescription, maybeGenre) =>
          makeArtist(name, facebookId, cover, websites, link, maybeDescription, maybeGenre)
        case (name, facebookId, "Artist", Some(cover: String), websites, link, maybeDescription, maybeGenre) =>
          makeArtist(name, facebookId, cover, websites, link, maybeDescription, maybeGenre)
      }
    }
    (facebookResponse.json \ "data")
      .asOpt[Seq[Artist]](collectOnlyArtistsWithCover)
      .getOrElse(Seq.empty)
  }

  def getFacebookArtistsByWebsites(websites: Set[String]): Future[Set[Option[Artist]]] = {
    Future.sequence(
      websites.map {
        case website if website contains "facebook" =>
          getFacebookArtistByFacebookUrl(website).map { maybeFacebookArtist => maybeFacebookArtist }
        case website if website contains "soundcloud" =>
          getMaybeFacebookUrlBySoundCloudUrl(website) flatMap {
            case None =>
              Future { None }
            case Some(facebookUrl) =>
              getFacebookArtistByFacebookUrl(facebookUrl).map { maybeFacebookArtist => maybeFacebookArtist }
          }
        case _ =>
          Future { None }
      }
    )
  }

  def getMaybeFacebookUrlBySoundCloudUrl(soundCloudUrl: String): Future[Option[String]] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/") + 1)
    WS.url("http://api.soundcloud.com/users/" + soundCloudName + "/web-profiles")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { readMaybeFacebookUrl }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: Response): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect {
        case (url: String, "facebook") => normalizeUrl(url)
      }
    }
    soundCloudWebProfilesResponse.json.asOpt[Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.length > 0 => Option(facebookUrls(0))
      case _ => None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[Artist]] = {
    //tinyUrl api??
    WS.url("https://graph.facebook.com/v2.2/" + normalizeFacebookUrl(url))
      .withQueryString(
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> token)
      .get()
      .map { readFacebookArtist }
  }

  def normalizeFacebookUrl(facebookUrl: String): String = {
    val firstNormalization = facebookUrl.drop(facebookUrl.lastIndexOf("/") + 1) match {
      case urlWithProfile: String if urlWithProfile contains "profile.php?id=" =>
        urlWithProfile.substring(urlWithProfile.lastIndexOf("=") + 1)
      case alreadyNormalizedUrl: String =>
        alreadyNormalizedUrl
    }
    firstNormalization match {
      case urlWithArguments if urlWithArguments contains "?" =>
        urlWithArguments.slice(0, urlWithArguments.lastIndexOf("?"))
      case urlWithoutArguments => urlWithoutArguments
    }
  }
  
  def readFacebookArtist(facebookResponse: Response): Option[Artist] = {
    facebookResponse.json
      .asOpt[(String, String, String, Option[String], Option[String],
        String, Option[String], Option[String])](readArtist)
      match {
        case Some((name, facebookId, "Musician/band", Some(cover: String), maybeWebsites,
        link, maybeDescription, maybeGenre)) =>
          Option(makeArtist(name, facebookId, cover, maybeWebsites, link, maybeDescription, maybeGenre))
        case Some((name, facebookId, "Artist", Some(cover: String), maybeWebsites,
        link, maybeDescription, maybeGenre)) =>
          Option(makeArtist(name, facebookId, cover, maybeWebsites, link, maybeDescription, maybeGenre))
        case _ => None
      }
  }

  def makeArtist(name: String, facebookId: String, cover: String, maybeWebsites: Option[String], link: String,
                 maybeDescription: Option[String], maybeGenre: Option[String]): Artist = {
    val facebookUrl = normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val websitesSet = getNormalizedWebsitesInText(maybeWebsites).filterNot(_.contains("facebook.com"))
    val description = formatDescription(maybeDescription)
    val genres = genresStringToGenresSet(maybeGenre)
    Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet, genres.toSeq,
      Seq.empty)
  }

  def genresStringToGenresSet(genres: Option[String]): Set[Genre] = genres match {
    case None => Set.empty
    case Some(genres: String) =>
      """([%/+,;]| - | & )""".r.split(genres.toLowerCase)
        .map { _.trim } match {
        case list if list.length != 1 => list.map { genreName =>
          new Genre(-1L, genreName.stripSuffix("."))
        }.toSet
        case listOfOneItem => listOfOneItem(0) match {
          case genre if genre.contains("'") || genre.contains("&") || genre.contains("musique") ||
            genre.contains("musik") || genre.contains("music") =>
            Set(new Genre(-1L, genre.stripSuffix(".")))
          case genreWithoutForbiddenChars =>
            genreWithoutForbiddenChars
              .split("\\s+")
              .map { genreName => new Genre(-1L, genreName.stripSuffix(".")) }
              .toSet
        }
      }
  }
}
