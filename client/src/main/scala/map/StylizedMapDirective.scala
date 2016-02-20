package map

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{ElementDirective, TemplatedDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("stylizedMap")
class StylizedMapDirective(timeout: Timeout) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/map/stylizedMap.html"

  override def controller = Option("mapController")
}
