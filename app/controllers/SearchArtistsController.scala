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

object SearchArtistsController extends Controller {
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
    ).apply((name: String, category: String, id: String, maybeCover: Option[String], website: Option[String],
             link: String, maybeDescription: Option[String], maybeGenre: Option[String]) =>
    (name, id, category, maybeCover, website, link, maybeDescription, maybeGenre))

  def readFacebookArtists(facebookResponse: Response): Seq[Artist] = {
    val collectOnlyArtistsWithCover: Reads[Seq[Artist]] = Reads.seq(readArtist).map { artists =>
      artists.collect {
        case (name, facebookId, "Musician/band", Some(cover: String),
        websites, link, maybeDescription, maybeGenre) =>
          val facebookUrl = Option(
            normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
          )
          val websitesSeq = facebookUrl match {
            case Some(facebookUrlFound) =>
              websitesStringToWebsitesSet(websites).filterNot(_ contains facebookUrlFound)
            case None =>
              websitesStringToWebsitesSet(websites)
          }
          val images = Set(new Image(-1, cover))
          val description = formatDescription(maybeDescription)
          val genres = genresStringToGenresSet(maybeGenre)
          Artist(-1, Option(facebookId), name, description, facebookUrl, websitesSeq, images, genres, Set.empty)
      }
    }

    (facebookResponse.json \ "data")
      .asOpt[Seq[Artist]](collectOnlyArtistsWithCover)
      .getOrElse(Seq.empty)
  }

  def getFacebookArtistByUrl(url: String): Future[Option[Artist]] = {
    val smallerUrl = url.replace("facebook.com/", "")
    val normalizedUrl =
      if (smallerUrl contains "/")
        smallerUrl.substring(smallerUrl.indexOf("/") + 1)
      else
        smallerUrl
    //println("normalizedUrl = " + normalizedUrl)
    WS.url("https://graph.facebook.com/v2.2/" + normalizedUrl)
      .withQueryString(
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> token)
      .get()
      .map { readFacebookArtist }
  }
  
  def readFacebookArtist(facebookResponse: Response): Option[Artist] = {
    facebookResponse.json
      .asOpt[(String, String, String, Option[String], Option[String],
        String, Option[String], Option[String])](readArtist)
      match {
        case Some((name, facebookId, "Musician/band", Some(cover: String), websites,
        link, maybeDescription, maybeGenre)) =>
          val facebookUrl = Option(
            normalizeUrl(link).substring("facebook.com/".length).replace("pages/", "").replace("/", "")
          )
          val websitesSeq = facebookUrl match {
            case Some(facebookUrlFound) =>
              websitesStringToWebsitesSet(websites).filterNot(_ contains facebookUrlFound)
            case None =>
              websitesStringToWebsitesSet(websites)
          }
          val images = Set(new Image(-1, cover))
          val description = formatDescription(maybeDescription)
          val genres = genresStringToGenresSet(maybeGenre)
          Option(Artist(-1, Option(facebookId), name, description, facebookUrl, websitesSeq, images, genres, Set.empty))
        case _ => None
      }
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
