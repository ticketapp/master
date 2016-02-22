package images

import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs.core.Timeout
import org.scalajs.dom.Element
import org.scalajs.dom.html.Html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("onErrorSrc")
class OnErrorSrcDirective(timeout: Timeout) extends AttributeDirective {

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
    elements.map(_.asInstanceOf[Html]).foreach { element =>
      Angular.element(element).bind("error", (error: js.Any) => {
        val onErrorSrc = element.getAttribute("data-on-error-src")
        element.setAttribute("src", onErrorSrc)
      })
    }
  }
}