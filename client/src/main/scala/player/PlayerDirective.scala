package player

import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core.Timeout
import events.HappeningWithRelations
import org.scalajs.dom.html.Html
import org.scalajs.dom.raw.Event
import org.scalajs.dom.{MouseEvent, clearInterval, document, setInterval}
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("playerControl")
class PlayerDirective(timeout: Timeout, playerService: PlayerService) extends ElementDirective with jsonHelper {

  val playerContainerElement: Html = document.getElementById("playerContainer").asInstanceOf[Html]
  val player = document.getElementById("player")
  var currentPlaylist: Seq[Track] = Seq.empty
  var currentPlaylistIndex = 0
  val durationElement: Html = player.getElementsByTagName("duration").item(0).asInstanceOf[Html]
  val trackTitleElement: Html = player.getElementsByTagName("track-title").item(0).asInstanceOf[Html]
  val trackArtistElement: Html = player.getElementsByTagName("artist-name").item(0).asInstanceOf[Html]
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
    playPauseElement.classList.add("paused")
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

  def secToTime(seconds: Double): String = {
    val minInSec = 60
    val hourInSec = 60*60
    val numberOfHours = Math.floor(seconds/hourInSec).toInt
    val numberOfMinutes = Math.floor((seconds - (numberOfHours * hourInSec))/minInSec).toInt
    val numberOfSeconds = seconds - (numberOfHours * hourInSec) - (numberOfMinutes * minInSec)

    val hoursToDisplay = if(numberOfHours == 0) ""
                         else if(numberOfHours < 10) "0" + numberOfHours + ":"
                         else numberOfHours + ":"

    val minutesToDisplay = if(numberOfMinutes == 0) "00:"
                           else if(numberOfMinutes < 10) "0" + numberOfMinutes + ":"
                           else numberOfMinutes + ":"

    val secondsToDisplay = if(numberOfSeconds == 0) "00"
                           else if(numberOfSeconds < 10) "0" + numberOfSeconds.toInt
                           else numberOfSeconds.toInt

    hoursToDisplay + minutesToDisplay + secondsToDisplay
  }

  def waitForDuration(): Unit = {
    playerService.getDuration() match {
      case duration if !duration.isNaN && duration > 0 =>
        durationElement.innerHTML = secToTime(playerService.getDuration())
      case _ =>
        timeout(() => waitForDuration() , 200)
    }
  }

  def setCurrentTime(time: String): Unit = {
    currentTimeElement.innerHTML = time
  }

  def setTrackInfo(track: Track): Unit = {
    val platform = if(track.platform == 's') "soundcloud"
                   else if (track.platform == 'y') "youtube"
    
    val url = track.redirectUrl match {
      case Some(redirectUrl) => redirectUrl
      case _ => "https://youtube.com/watch?v=" + track.url
    }
    
    trackTitleElement.innerHTML = track.title
    trackArtistElement.innerHTML ="<a class=\"redirect-icon\" href=\"" + url + "\" target=\"_blank\">" +
                                    "<i class=\"fa fa-" + platform + "\"></i>" +
                                  "</a>" + track.artistName
  }

  @JSExport
  def setEventPlaylist(jsEvent: js.Any): Unit = {
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
    playPauseElement.classList.remove("paused")
    playPauseElement.onclick = (event: MouseEvent) => {
      pause()
    }
  }

  def setCurrentIndex(track: Track): Unit = {
    currentPlaylistIndex = currentPlaylist.indexOf(track)
  }

  def enableNextAndPrev: Unit = {
    if (currentPlaylistIndex == 0) prevElement.classList.add("disabled")
    else prevElement.classList.remove("disabled")

    if (currentPlaylistIndex >= currentPlaylist.length - 1) nextElement.classList.add("disabled")
    else nextElement.classList.remove("disabled")
  }

  def showPlayer: Unit = {
    playerContainerElement.classList.remove("ng-hide")
    playerContainerElement.classList.add("showPlayer")
    timeout(() => {
      playerContainerElement.classList.remove("showPlayer")
      playerContainerElement.setAttribute("ng-show", "true")
    }, 2000, true)
  }

  def setPlayerListeners: Unit = {
    val currentPlayer = playerService.getCurrentPlayer
    val timeIntervalInMillisecond = 1000
    val setCurrentTimeInterval = setInterval(() => setCurrentTime(secToTime(playerService.getCurrentTime())), timeIntervalInMillisecond)
    currentPlayer.onended = (event: Event) => {
      clearInterval(setCurrentTimeInterval)
      next()
    }
  }

  def playTrack(track: Track): Unit = {
    playerService.setTrack(track)
    play()
    setCurrentIndex(track)
    enableNextAndPrev
    waitForDuration()
    setTrackInfo(track)
    setPlayerListeners
    showPlayer
  }

}