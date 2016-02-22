package auth

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{Attributes, ElementDirective, injectable}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Html
import org.scalajs.dom.raw.MouseEvent

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("auth")
class AuthDirective(timeout: Timeout, auth: Auth) extends ElementDirective {

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
    elements.map(_.asInstanceOf[Html]).foreach { element =>
        element.onclick = (event: MouseEvent) => auth.authenticate("facebook")
    }
  }
}