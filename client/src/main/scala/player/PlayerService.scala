package player

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}
import events.HappeningWithRelations
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLMediaElement, HTMLSourceElement}
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON

@js.native
class MediaElementPlayer(id: String) extends HTMLMediaElement {
  def setSrc(url: String): js.Any = js.native
  val media: HTMLMediaElement = js.native
}

@injectable("playerService")
class PlayerService(http: HttpService) extends Service with jsonHelper {
  require(http != null, "Missing argument 'http'.")

  val soundcloudClientId: String = "f297807e1780623645f8f858637d4abb"
  val videoPlayer = document.getElementById("videoPlayer").getElementsByTagName("source").item(0).asInstanceOf[HTMLSourceElement]
  val musicPlayer = document.getElementById("musicPlayer").asInstanceOf[MediaElementPlayer]
  var currentPlayer = musicPlayer

  def getTracksByArtistFacebookUrl(facebookUrl: String, numberToReturn: Int, offset: Int): Future[Seq[Track]] = {
    http.get[js.Any](PlayerRoutes.findByArtist(facebookUrl, numberToReturn, offset)) map { tracks =>
      read[Seq[Track]](JSON.stringify(tracks))
    }
  }

  def getEventPlaylist(event: HappeningWithRelations): Future[Seq[Track]] = {
    val collectedArtists = event.artists collect {
      case artistWithTracks if artistWithTracks.artist.hasTracks => artistWithTracks.artist
    }

    val eventuallyEventPlaylist = collectedArtists.toSeq map { artist =>
      val numberToReturn: Int = 5
      val offset: Int = 0
      getTracksByArtistFacebookUrl(artist.facebookUrl, numberToReturn, offset)
    }

    Future.sequence(eventuallyEventPlaylist) map(_.flatten)
  }

  def getDuration(): Double = currentPlayer.media.duration

  def getCurrentTime(): Double = currentPlayer.media.currentTime

  def pause(): Unit = currentPlayer.pause()

  def play(): Unit = currentPlayer.play()

  def getCurrentPlayer: HTMLMediaElement = currentPlayer

  def setTrack(track: Track): Unit = {
    track.platform match {
      case soundcloud if soundcloud == 's' =>
        musicPlayer.src = track.url + "?client_id=" + soundcloudClientId
        currentPlayer.pause()
        currentPlayer =  new MediaElementPlayer("#musicPlayer")

      case youtube if youtube == 'y' =>
        videoPlayer.src = "http://youtube.com/watch?v=" + track.url
        currentPlayer.pause()
        currentPlayer = new MediaElementPlayer("#videoPlayer")
    }
  }
}

@injectable("playerService")
class PlayerServiceFactory(http: HttpService) extends Factory[PlayerService] {

  override def apply(): PlayerService = new PlayerService(http)
}
