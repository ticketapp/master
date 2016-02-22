package map

import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core.Timeout

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("stylizedMap")
class StylizedMapDirective(timeout: Timeout) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/map/stylizedMap.html"

  override def controller = Option("mapController")
}
