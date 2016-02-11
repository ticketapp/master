package tracking


import java.util.UUID

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs
import org.scalajs.dom.{WindowLocalStorage, Element}
import org.scalajs.dom.html.Html
import upickle.default._
import utilities.{CookiesOptions, NgCookies}
import org.scalajs.dom.console
import scala.scalajs.js.{Object, Date, JSON}
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("tracking")
class TrackingDirective(timeout: Timeout, ngCookies: NgCookies) extends AttributeDirective {

  var sessionId = 0
  val storedActions: Seq[Action] = Seq.empty[Action]
  val cookie = ngCookies.get("sessionId") match {
    case string if string.isInstanceOf[String] =>
      string.asInstanceOf[String].toInt
    case _ =>
      // postSession
      ngCookies.put("sessionId", 152)
      //post stored actions
      152
  }

  @JSExport
  def track(action: String): Unit = {
    val newAction = Action(action, new Date().getDate(), sessionId)
    if(sessionId > 0) console.log(action)
    else storedActions :+ newAction
  }

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map{_.asInstanceOf[Html]}.foreach { element =>

        }
    }

}
