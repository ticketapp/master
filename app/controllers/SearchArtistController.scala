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
import services.Utilities.{ normalizeUrl, normalizeString }
import models.Artist
import models.Image
import models.Genre

object SearchArtistController extends Controller {
  val token = play.Play.application.configuration.getString("facebook.token")

  def websitesStringToWebsitesSet(websites: Option[String]): Set[String] = websites match {
    case None => Set.empty
    case Some(websites: String) =>
      """((https?:\/\/(www\.)?)|www\.)""".r.split(websites.toLowerCase)
        .map { _.trim.stripSuffix("/") }
        .toSet
  }

  def genresStringToGenresSet(genres: Option[String]): Set[String] = genres match {
    case None => Set.empty
    case Some(genres: String) =>
      """([%/+,;]| - | & )""".r.split(genres.toLowerCase)
        .map { _.trim } match {
        case list if list.length != 1 => list.toSet
        case listOfOneItem => listOfOneItem(0) match {
          case genre if genre.contains("'") => Set(genre)  //contains several chars?
          case genreWithoutForbiddenChars => genreWithoutForbiddenChars.split("\\s+").toSet
        }
      }
  }

  def readFacebookArtist(facebookResponse: Response): Seq[Artist] = {
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


    val readArtistsWithCover: Reads[Seq[Artist]] = Reads.seq(readArtist).map { artists =>
      artists.collect {
        case (name, facebookId, "Musician/band", Some(cover: String), websites, link, maybeDescription, maybeGenre) =>
          val websitesSeq = websitesStringToWebsitesSet(websites) + normalizeUrl(link)
          val images = Set(new Image(-1, cover))
          val description = formatDescription(maybeDescription)
          val genres = maybeGenre match {
            case Some(genre: String) => Set(new Genre(-1, genre))
            case None => Set.empty[Genre]
          }
          Artist(-1, Option(facebookId), name, description, websitesSeq, images, genres, Set.empty)
      }
    }

    (facebookResponse.json \ "data")
      .asOpt[Seq[Artist]](readArtistsWithCover)
      .getOrElse(Seq.empty)
  }


  def get20FacebookArtists(pattern: String): Future[Seq[Artist]] = {
    WS.url("https://graph.facebook.com/v2.2/search")
      .withQueryString(
        "q" -> pattern,
        "access_token" -> token,
        "type" -> "page",
        "limit" -> "400",
        "fields" -> "name,cover{source},id,category,link,website,description,genre")
      .get()
      .map { readFacebookArtist(_).take(20) }
  }


  def getFacebookArtistsContaining(pattern: String) = Action.async {
    val sanitizedPattern = normalizeString(pattern.replaceAll(" ", "+"))

    get20FacebookArtists(sanitizedPattern).map { artists =>
      Ok(Json.toJson(artists))
    }
  }
}
