package Contact

import com.greencatsoft.angularjs.extensions.material.Sidenav
import com.greencatsoft.angularjs.{TemplatedDirective, ElementDirective, injectable}

import scala.scalajs.js.annotation.JSExport


@JSExport
@injectable("contactComponent")
class ContactComponentDirective(sidenav: Sidenav) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/Contact/contact-component.html"

  @JSExport
  def toggleRight() = {
    sidenav("right").toggle()
  }
}
