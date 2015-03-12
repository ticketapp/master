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
import services.Utilities.normalizeUrl
import models.Artist
import models.Image
import models.Genre
import play.api.libs.json.Reads._

object SearchArtistsController extends Controller {
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
  val token = play.Play.application.configuration.getString("facebook.token")

  def getFacebookArtistsContaining(pattern: String) = Action.async {
    get20FacebookArtists(pattern).map { artists =>
      Ok(Json.toJson(artists))
    }
  }

  def get20FacebookArtists(pattern: String): Future[Seq[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/search")
      .withQueryString(
        "q" -> pattern,
        "type" -> "page",
        "limit" -> "400",
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> token)
      .get()
      .map { readFacebookArtists(_).take(20) }
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
          getFacebookArtistByFacebookUrl(website).map { maybeFacebookArtist => maybeFacebookArtist } //bySoundcloudUrl
        case _ =>
          Future { None }
      }
    )
  }

  def getFacebookArtistBySoundCloudUrl(soundCloudUrl: String)={//: Future[Option[Artist]] = {
    val soundCloudName = soundCloudUrl.substring(soundCloudUrl.indexOf("/"))
    WS.url("http://api.soundcloud.com/users/" + soundCloudName + "/web-profiles")
      .withQueryString("client_id" -> soundCloudClientId)
      .get()
      .map { response =>
        println(readMaybeFacebookUrl(response))
        println(response.json)
    }
  }

  def readMaybeFacebookUrl(soundCloudWebProfilesResponse: Response): Option[String] = {
    val facebookUrlReads = (
      (__ \ "url").read[String] and
        (__ \ "service").read[String]
      )((url: String, service: String) => (url, service))

    val collectOnlyFacebookUrls = Reads.seq(facebookUrlReads).map { urlService =>
      urlService.collect {
        case (url: String, "facebook") => url
      }
    }
    soundCloudWebProfilesResponse.json.asOpt[Seq[String]](collectOnlyFacebookUrls) match {
      case Some(facebookUrls: Seq[String]) if facebookUrls.length > 0 => Option(facebookUrls(0))
      case _ => None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[Artist]] = {
    val smallerUrl = url.replace("facebook.com/", "")
    val normalizedUrl =
      if (smallerUrl contains "/")
        smallerUrl.substring(smallerUrl.indexOf("/", 2) + 1)
      else
        smallerUrl
    WS.url("https://graph.facebook.com/v2.2/" + normalizedUrl)
      .withQueryString(
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> token)
      .get()
      .map { readFacebookArtist }
  }
  
  def readFacebookArtist(facebookResponse: Response): Option[Artist] = {
    //println(facebookResponse.json)
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
    //println(name, facebookId, maybeWebsites, link, maybeDescription, maybeGenre)
    val facebookUrl = normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
    val websitesSeq = websitesStringToWebsitesSet(maybeWebsites).filterNot(_ contains facebookUrl)
    val images = Set(new Image(-1, cover))
    val description = formatDescription(maybeDescription)
    val genres = genresStringToGenresSet(maybeGenre)
    Artist(-1, Option(facebookId), name, description, facebookUrl, websitesSeq, images, genres, Seq.empty)
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

  def websitesStringToWebsitesSet(websites: Option[String]): Set[String] = websites match {
    case None => Set.empty
    case Some(websites: String) =>
      """((https?:\/\/(www\.)?)|www\.)""".r.split(websites.toLowerCase)
        .map { _.trim.stripSuffix("/") }
        .filter(_.nonEmpty)
        .toSet
  }
}
