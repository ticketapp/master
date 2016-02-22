package events

import com.greencatsoft.angularjs.core.{Window, Timeout}
import com.greencatsoft.angularjs.{Attributes, ElementDirective, TemplatedDirective, injectable}
import org.scalajs.dom.{Event, Element}
import org.scalajs.dom.html.Html
import org.scalajs.dom.setTimeout
import org.scalajs.dom.clearTimeout
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("eventMin")
class EventMinDirective(timeout: Timeout, window: Window) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/events/eventMin.html"

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map(_.asInstanceOf[Html]).foreach { element =>

          def resize(): Unit = {
            val elemWidth = element.getBoundingClientRect().width
            if(elemWidth > 50) {
              val heightRatio: Double = 0.35
              element.style.height = Math.round(elemWidth * heightRatio) + "px"
            }
            else {
              timeout(() => resize(), 150)
            }
          }

          resize()

          var timer = setTimeout(() => resize(), 400)
          val listener = (event: Event) => {
            clearTimeout(timer)
            timer = setTimeout(() => resize(), 400)
          }

          window.addEventListener("resize", listener)
        }
    }
}
