package footer


import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{TemplatedDirective, Attributes, ClassDirective, injectable}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Html

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("landingPageFooter")
class LandingPageFooterDirective(timeout: Timeout) extends ClassDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/footer/footer.html"

}