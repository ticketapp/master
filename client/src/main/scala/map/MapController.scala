package map

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("mapController")
class MapController(mapScope: MapScope, timeout: Timeout) extends AbstractController[MapScope](mapScope) {
  val baseGeographicPoint = "45.7676,4.8350"

  mapScope.zoom = 15
  mapScope.geographicPoint = baseGeographicPoint
  mapScope.travelMode = "DRIVING"

  def setCenter(newGeographicPoint: String): Unit = timeout(() => mapScope.geographicPoint = newGeographicPoint)

  def setTravelMode(newTravelMode: String) = timeout(() => mapScope.travelMode = newTravelMode)
}
