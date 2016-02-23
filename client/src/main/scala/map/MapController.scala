package map

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("mapController")
class MapController(mapScope: MapScope, timeout: Timeout) extends AbstractController[MapScope](mapScope) {
  mapScope.zoom = 15
  mapScope.travelMode = "DRIVING"

  def setTravelMode(newTravelMode: String) = timeout(() => mapScope.travelMode = newTravelMode)
}
