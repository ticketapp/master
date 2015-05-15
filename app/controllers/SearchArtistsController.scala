package controllers

import json.JsonHelper._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import services.Utilities
import models.Artist
import models.Genre
import models.Genre.genresStringToGenresSet
import play.api.libs.json.Reads._
import services.Utilities.{ normalizeUrl, getNormalizedWebsitesInText }

object SearchArtistsController extends Controller {
  val soundCloudClientId = "f297807e1780623645f8f858637d4abb"
  val facebookToken = "1434769156813731%7Cf2378aa93c7174712b63a24eff4cb22c"
  val linkPattern = """((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-z@A-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)""".r

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
        "access_token" -> facebookToken)
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
      case Some(facebookUrls: Seq[String]) if facebookUrls.length > 0 => Option(facebookUrls.head)
      case _ => None
    }
  }

  def getFacebookArtistByFacebookUrl(url: String): Future[Option[Artist]] = {
    //tinyUrl api??
    WS.url("https://graph.facebook.com/v2.2/" + normalizeFacebookUrl(url))
      .withQueryString(
        "fields" -> "name,cover{source},id,category,link,website,description,genre",
        "access_token" -> facebookToken)
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
    val websitesSet = getNormalizedWebsitesInText(maybeWebsites)
      .filterNot(_.contains("facebook.com"))
      .filterNot(_ == "")
    val description = Utilities.formatDescription(maybeDescription)
    val genres = genresStringToGenresSet(maybeGenre)
    Artist(None, Option(facebookId), name, Option(cover), description, facebookUrl, websitesSet, genres.toSeq, Seq.empty)
  }

  def removeUselessInSoundCloudWebsite(website: String): String = website match {
    case soundCloudWebsite if soundCloudWebsite contains "soundcloud" =>
      soundCloudWebsite.take(soundCloudWebsite.lastIndexOf('/'))
    case _ => website
  }
}
