package geolocation

import org.scalajs.dom.navigator
import org.scalajs.dom.raw.Position
import org.scalajs.dom.console


trait GeolocationService {
  def getHtmlGeolocation = {
    navigator.geolocation.getCurrentPosition((position: Position) => {
      console.log(position)
    })
  }
}