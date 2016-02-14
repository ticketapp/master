package focusDirective


import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs._
import org.scalajs.dom.Element
import org.scalajs.dom.html.{Input, Html}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("focus")
class FocusDirective(timeout: Timeout) extends AttributeDirective {

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map{_.asInstanceOf[Html]}.foreach { element =>
          attributes.$observe("focus", (value: String) => {
            if(value.toBoolean) {
              element match {
                case _: Input => element.focus()
                case _ => element.getElementsByTagName("input").item(0).asInstanceOf[Html].focus()
              }
            }
          })
        }
    }

}