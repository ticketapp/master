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
  case class FacebookArtist(id: String,
                            cover: String,
                            website: List[String],
                            link: String,
                            soundCloudTracks: List[SoundCloudTrack] = List())
  implicit val facebookArtistWrites: Writes[FacebookArtist] = Json.writes[FacebookArtist]

  case class SoundCloudTrack(stream_url: String,
                             title: String,
                             artwork_url: Option[String] )
  implicit val soundCloudTrackWrites: Writes[SoundCloudTrack] = Json.writes[SoundCloudTrack]

  val token = play.Play.application.configuration.getString("facebook.token")
  val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")

  def websitesStringToWebsitesList(websites: Option[String]): List[String] = {
    var listOfWebSitesToReturn: List[String] = List()
    websites match {
      case None => List()
      case Some(websitesFound) =>
        for (website <- websitesFound.split(" ").toList) {
          listOfWebSitesToReturn = listOfWebSitesToReturn ::: website.split("www.").toList
            .filter(_ != "").filter(_ != "http://").filter(_ != "https://")
        }
        listOfWebSitesToReturn
    }
  }

  def findFacebookArtists(pattern: String): Future[Seq[FacebookArtist]] = {
    //tout enlever pour mettre un implicit val reads
    val readCategory: Reads[String] = (__ \ "category").read[String]
    val readId: Reads[String] = (__ \ "id").read[String]
    val readCoverSource: Reads[String] = (__ \ "source").read[String]
    val readOptionalCover: Reads[Option[String]] = (__ \ "cover").readNullable(readCoverSource)
    val readWebsites: Reads[Option[String]] = (__ \ "website").readNullable
    val readLink: Reads[String] = (__ \ "link").read[String]
    val readAllArtist: Reads[(String, String, Option[String], Option[String], String)] =
      readId.and(readCategory).and(readOptionalCover).and(readWebsites).and(readLink)
        .apply((id: String, category: String, maybeCover: Option[String], website: Option[String], link: String)
      => (id, category, maybeCover, website, link))
    val readArtistsArray: Reads[Seq[(String, String, Option[String], Option[String], String)]] = Reads.seq(readAllArtist)
    val collectOnlyMusiciansWithCover: Reads[Seq[(String, String, Option[String], String)]] = readArtistsArray.map {
      pages =>
        pages.collect{ case (id, "Musician/band", Some(cover), websites, link) => (id, cover, websites, link) }
    }
    val readArtists: Reads[Seq[FacebookArtist]] = collectOnlyMusiciansWithCover.map { artists =>
      artists.map{ case (id, cover, websites, link) =>
        FacebookArtist(id, cover, websitesStringToWebsitesList(websites), link)
      }
    }

    WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=400&type=page&fields=name,cover%7Bsource%7D,id,category,likes,link,website&access_token=" + token).get
      .map { response =>
      (response.json \ "data").as[Seq[FacebookArtist]](readArtists)
    }
  }

  implicit val soundCloudTracksReads = Json.reads[SoundCloudTrack]
 /* implicit val soundCloudTracksReads: Reads[SoundCloudTrack] = (
    (JsPath \ "stream_url").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "artwork_url").read[Option[String]]
    )(SoundCloudTrack.apply _)*/


  def findSoundCloudTracks(artist: FacebookArtist): Future[FacebookArtist] = {
    /*val readStream_url: Reads[String] = (__ \ "stream_url").read[String]
    val readTitle: Reads[String] = (__ \ "title").read[String]
    val readArtwork_url: Reads[Option[String]] = (__ \ "artwork_url").readNullable
    val readAllTrack: Reads[(String, String, Option[String])] =
      readStream_url.and(readTitle).and(readArtwork_url)
        .apply((readStream_url: String, readTitle: String, readArtwork_url: Option[String])
      => (readStream_url, readTitle, readArtwork_url))
    val readTracksArray: Reads[List[(String, String, Option[String])]] = Reads.list(readAllTrack)
    val readTracks1: Reads[List[SoundCloudTrack]] = readTracksArray.map { tracks =>
      tracks.map{ case (stream_url, title, artwork_url) => SoundCloudTrack(stream_url, title, artwork_url) }
    }*/

    val readTracks: Reads[List[SoundCloudTrack]] = Reads.list(soundCloudTracksReads)

    var soundCloudLink: String = ""
    for (site <- artist.website) {
      site.indexOf("soundcloud.com") match {
        case -1 =>
        case i => soundCloudLink = site.substring(i + 15) //15 = "soundcloud.com".length
      }
    }

    soundCloudLink match {
      case "" => Future { artist }
      case scLink => WS.url("http://api.soundcloud.com/users/" + scLink + "/tracks?client_id=" + soundCloudClientId)
        .get.map { soundCloudTracks =>
        artist.copy(soundCloudTracks = soundCloudTracks.json.as[List[SoundCloudTrack]](readTracks))
      }
    }
  }


  def test(pattern: String): Future[Seq[Future[FacebookArtist]]] = {
    findFacebookArtists(pattern).map { facebookArtists: Seq[FacebookArtist] =>
      //println(Json.toJson(facebookArtists))
      facebookArtists.map { facebookArtist =>
        findSoundCloudTracks(facebookArtist)
      }
    }
  }

  def test2(pattern: String) = Action {

    println(Json.toJson(SoundCloudTrack("ljkjk", "ljklkj", Some("lkjljk"))))
    Ok(Json.toJson(FacebookArtist("lkj", "lkjkl", List("lkj"), "lkjk",
      List())) )
    //SoundCloudTrack("ljkjk", "ljklkj", Some("lkjljk"))


    /*val testReturned: Future[Seq[Future[FacebookArtist]]] = test(pattern)
    testReturned.flatMap { a: Seq[Future[FacebookArtist]] =>
      val b = Future.sequence(a): Future[Seq[FacebookArtist]]
      b.map { c: Seq[FacebookArtist] =>
        //println(Json.toJson(c))
        println(c)
        Ok
      }
    }*/

/*
    var facebookArtistCompleted: List[FacebookArtist] = List()
    var toBeReturned: List[FacebookArtist] = List()

    findFacebookArtists(pattern).map { facebookArtists: Seq[FacebookArtist] =>
      facebookArtists.map { facebookArtist: FacebookArtist =>
        //toBeReturned ::= facebookArtist
        //println(toBeReturned.length)
          findSoundCloudTracks(facebookArtist).map { facebookArtistWSoundCloudTracks: FacebookArtist => //println(facebookArtistWSoundCloudTracks)
            facebookArtistCompleted ::= facebookArtistWSoundCloudTracks
            println(Json.toJson(facebookArtistCompleted))
            //facebookArtistWSoundCloudTracks
          }
      }

      Ok(Json.toJson(facebookArtistCompleted))

    }
*/


    /*
        /*
        WS.url("http://api.soundcloud.com/users/johnyalen/tracks?client_id=" + soundCloudClientId).get.map { response =>
          val tracksToReturn = response.json.as[Seq[SoundCloudTrack]](readTracks)
          for (track <- tracksToReturn) {
            track.artwork_url match {
              case Some(artwork_url) =>
              case None => //On va chercher l'image de l'user
            }
          }
          Ok(Json.toJson(tracksToReturn))
        }*/

        val readSoundCloudIds: Reads[Seq[Long]] = Reads.seq((__ \ "id").read[Long])

        /*WS.url("http://api.soundcloud.com/users?q=jonny&client_id=" + soundCloudClientId).get.map {
          response => println(response.json.as[Seq[Long]](readSoundCloudIds))
            Ok
        }*/


        val listSoundCloudIds = List(44754718, 424376, 23008, 871737, 4507106, 37010262, 21683946, 6837207, 4674475,
          7845914, 534200, 1278145, 51389792, 32179999, 23436369, 377909, 9262460, 5810328, 14124498, 12844042, 3400756,
          31701859, 19463719, 66629, 70872423, 32858173, 245119, 1410509, 31313552, 94422, 22278231, 962651, 61839306,
          2477273, 25424932, 19670762, 11833741, 38769243, 2356770, 13922620, 22914804, 96867217, 1376901, 40644, 29030604,
          1267122, 6741016, 12040018, 21554984, 15162494)


        val readUrls: Reads[Seq[Option[String]]] = Reads.seq((__ \ "url").readNullable)


        for (id <- listSoundCloudIds) {
          WS.url("http://api.soundcloud.com/users/" + id + "/web-profiles?client_id=" + soundCloudClientId).get.map {
            response => //println(response.json)
              val websites = (id, response.json.as[Seq[Option[String]]](readUrls).flatten)
              println(websites)

          }
        }



    /*
        WS.url("http://api.soundcloud.com/users?q=jonny&client_id=" + soundCloudClientId).get.map {
          response => Ok
        }*/
        */
  }


  def test2 = Action {
    Ok("Okay\n")
  }
}

