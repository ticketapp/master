package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Failure, Success, Try}
import play.api.libs.functional.syntax._

object Test2 extends Controller {
  case class Category(id: String, cover: String, webSite: Option[String])
  implicit val artistToReturnWrites: Writes[Category] = Json.writes[Category]

  case class TrackToReturn(stream_url: String, title: String, artwork_url: Option[String])
  implicit val TrackToReturnWrites: Writes[TrackToReturn] = Json.writes[TrackToReturn]

  val token = play.Play.application.configuration.getString("facebook.token")
  val soundCloudClientId = play.Play.application.configuration.getString("soundcloud.clientId")

  def test2(pattern: String) = Action.async {
    val readCategory: Reads[String] = (__ \ "category").read[String]
    val readId: Reads[String] = (__ \ "id").read[String]
    val readCoverSource: Reads[String] = (__ \ "source").read[String]
    val readOptionalCover: Reads[Option[String]] = (__ \ "cover").readNullable(readCoverSource)
    val readWebSite: Reads[Option[String]] = (__ \ "website").readNullable
    val readAllArtist: Reads[(String, String, Option[String], Option[String])] =
      readId.and(readCategory).and(readOptionalCover).and(readWebSite)
        .apply((id: String, category: String, maybeCover: Option[String], webSite: Option[String])
      => (id, category, maybeCover, webSite))
    val readArtistsArray: Reads[Seq[(String, String, Option[String], Option[String])]] = Reads.seq(readAllArtist)
    val collectOnlyMusiciansWithCover: Reads[Seq[(String, String, Option[String])]] = readArtistsArray.map { pages =>
        pages.collect{ case (id, "Musician/band", Some(cover), webSite) => (id, cover, webSite) }
    }
    val readArtists: Reads[Seq[Category]] = collectOnlyMusiciansWithCover.map { artists =>
      artists.map{ case (id, cover, webSite) => Category(id, cover, webSite) }
    }


    /*WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=30&type=page&fields=name,cover%7Bsource%7D,id,category,likes,link,website&access_token=" + token).get
      .map { response =>
        val artists = (response.json \ "data").as[Seq[Category]](readArtists)
        for (artist <- artists) {
          artist.webSite match {
            case None =>
            case Some(site) => val listSites: List[String] = site.split("\n").toList
              for (site <- listSites) {
                site.indexOf("soundcloud.com") match {
                  case -1 =>
                  case i => WS.url("http://api.soundcloud.com/users/" + site.substring(i + 15) //15 = "soundcloud.com".length
                      + "/tracks?client_id=" + soundCloudClientId).get.map { response =>
                      println(response.json)
                    }
                }
              }
          }
        }
      Ok(Json.toJson((response.json \ "data").as[Seq[Category]](readArtists)))
    }*/


    val readStream_url: Reads[String] = (__ \ "stream_url").read[String]
    val readTitle: Reads[String] = (__ \ "title").read[String]
    val readArtwork_url: Reads[Option[String]] = (__ \ "artwork_url").readNullable

    val readAllTrack: Reads[(String, String, Option[String])] =
      readStream_url.and(readTitle).and(readArtwork_url)
        .apply((readStream_url: String, readTitle: String, readArtwork_url: Option[String])
      => (readStream_url, readTitle, readArtwork_url))

    val readTracksArray: Reads[Seq[(String, String, Option[String])]] = Reads.seq(readAllTrack)

    /*val collectOnlyMusiciansWithCover: Reads[Seq[(String, String, Option[String])]] = readAnArray.map { pages =>
        pages.collect{ case (id, "Musician/band", Some(cover), webSite) => (id, cover, webSite) }
    }*/

    val readTracks: Reads[Seq[TrackToReturn]] = readTracksArray.map { tracks =>
      tracks.map{ case (stream_url, title, artwork_url) => TrackToReturn(stream_url, title, artwork_url) }
    }


    WS.url("http://api.soundcloud.com/users/johnyalen/tracks?client_id=" + soundCloudClientId).get.map { response =>
      /*val readErrors: Reads[Option[String]] = (__ \ "errors").readNullable
      println("lkjkljkl")
      readErrors.reads(response.json).map {
        case Some(errorFound) => println("akljlkjkljlk")
        case _ => println("x :")
      }*/
        /*match
        {
          case None => println("qeerr")
          case _ => println("qeqsdqserr")
        }*/

      /*val tracksToReturn = response.json.as[Seq[TrackToReturn]](readTracks)
      for (track <- tracksToReturn) {
        track.artwork_url match {
          case Some(artwork_url) =>
          case None => //On va chercher l'image de l'user
        }
      }
      Ok(Json.toJson(tracksToReturn))*/
      Ok
    }

  }




/*
  def test2 = Action {
    Ok("Okay\n")
  }*/
}

