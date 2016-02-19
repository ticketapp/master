package player

import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core.Timeout
import events.HappeningWithRelations
import org.scalajs.dom.html.Html
import org.scalajs.dom.raw.Event
import org.scalajs.dom.{MouseEvent, document}
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("playerControl")
class PlayerDirective(timeout: Timeout, playerService: PlayerService) extends ElementDirective with jsonHelper {

  var currentPlaylist: Seq[Track] = Seq.empty
  val player = document.getElementById("player")
  var currentPlaylistIndex = 0
  val durationElement: Html = player.getElementsByTagName("duration").item(0).asInstanceOf[Html]
  val infoElement: Html = player.getElementsByTagName("info").item(0).asInstanceOf[Html]
  val nextElement: Html = player.getElementsByTagName("next").item(0).asInstanceOf[Html]
  val prevElement: Html = player.getElementsByTagName("prev").item(0).asInstanceOf[Html]
  val playPauseElement: Html = player.getElementsByTagName("play-pause").item(0).asInstanceOf[Html]
  val currentTimeElement: Html = player.getElementsByTagName("current-time").item(0).asInstanceOf[Html]

  nextElement.onclick =  (event: MouseEvent) => {
    next()
  }

  prevElement.onclick =  (event: MouseEvent) => {
    prev()
  }

  def passPlayBtnToPlay(): Unit = {
    playPauseElement.classList.add("fa-play-circle-o")
    playPauseElement.classList.remove("fa-pause")  
  }
  
  def next(): Unit = {
    if(currentPlaylistIndex < currentPlaylist.length -1)
      passPlayBtnToPlay()
      playTrack(currentPlaylist(currentPlaylistIndex + 1))
  }

  def prev(): Unit = {
    if(currentPlaylistIndex > 0)
      passPlayBtnToPlay()
      playTrack(currentPlaylist(currentPlaylistIndex - 1))
  }

  def waitForDuration(): Unit = {
    playerService.getDuration() match {
      case duration if !duration.isNaN =>
        durationElement.innerHTML = playerService.getDuration().toString
      case _ =>
        timeout(() => waitForDuration() , 200)
    }
  }

  def setTrackInfo(track: Track): Unit = {
    infoElement.innerHTML = track.title
  }

  @JSExport
  def playEventPlaylist(jsEvent: js.Any): Unit = {
    val event = read[HappeningWithRelations](JSON.stringify(jsEvent))
    playerService.getEventPlaylist(event) map { playlist =>
      currentPlaylist = playlist
      playTrack(playlist.head)
    }
  }

  def pause(): Unit = {
    playerService.pause()
    passPlayBtnToPlay()
    playPauseElement.onclick = (event: MouseEvent) => {
      play()
    }
  }

  def play(): Unit = {
    playerService.play()
    playPauseElement.classList.add("fa-pause")
    playPauseElement.classList.remove("fa-play-circle-o")
    playPauseElement.onclick = (event: MouseEvent) => {
      pause()
    }
  }

  def setCurrentIndex(track: Track): Unit = {
    currentPlaylistIndex = currentPlaylist.indexOf(track)
    if (currentPlaylistIndex == 0) prevElement.classList.add("ng-hide")
    else prevElement.classList.remove("ng-hide")
    if (currentPlaylistIndex >= currentPlaylist.length - 1) nextElement.classList.add("ng-hide")
    else nextElement.classList.remove("ng-hide")
  }

  def playTrack(track: Track): Unit = {
    playerService.setTrack(track)
    play()
    setCurrentIndex(track)
    waitForDuration()
    setTrackInfo(track)
    playerService.getCurrentPlayer.onended = (event: Event) => {
      next()
    }
  }
}