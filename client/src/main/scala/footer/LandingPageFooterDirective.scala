package footer

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{ClassDirective, TemplatedDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("landingPageFooter")
class LandingPageFooterDirective(timeout: Timeout) extends ClassDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/footer/footer.html"
}