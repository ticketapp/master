package map

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{TemplatedDirective, ElementDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("mapControls")
class MapControlsDirective(timeout: Timeout) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/map/mapControls.html"

  override def controller = Option("mapController")
}
