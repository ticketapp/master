package tracking


import java.util.UUID
import javafx.scene.input.ScrollEvent

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom._
import org.scalajs.dom.html.Html
import upickle.Js
import upickle.default._
import utilities.{CookiesOptions, NgCookies}
import scala.scalajs.js
import scala.scalajs.js.{Object, Date, JSON}
import scala.scalajs.js.annotation.JSExport
import scala.concurrent.ExecutionContext.Implicits.global

trait MousePosition extends js.Object {
  var left:Double = js.native
  var top:Double = js.native
}

@JSExport
@injectable("tracking")
class TrackingDirective(timeout: Timeout, ngCookies: NgCookies, httpService: HttpGeneralService) extends ClassDirective {

  val storedActions: Seq[Action] = Seq.empty[Action]
  var sessionId = ""
  val cookie = ngCookies.get("sessionId") match {
    case string if string.isInstanceOf[String] =>
      sessionId = string.asInstanceOf[String]
    case _ =>
      httpService.post(TrackingRoutes.postSession(screenWidth = window.innerWidth, screenHeight = window.innerHeight)) map { newSessionId =>
        val newId = read[String](newSessionId)
        ngCookies.put("sessionId", newId)
        sessionId = newId
        storedActions map { action =>
          httpService.postWithObject(TrackingRoutes.postWithActionObject, write(action))
        }

      }
  }

  var mousePosition = new Object().asInstanceOf[MousePosition]
  mousePosition.left = 0
  mousePosition.top = 0
  val doc = document.getElementsByTagName("md-content").item(0).asInstanceOf[Html]

  var scrollTop = 0.0

  @JSExport
  def track(action: String): Unit = {
    val newDate = new Date().getTime()
    val newAction = Action(action + "," + doc.scrollTop, newDate, sessionId)
    if(sessionId.length > 0) httpService.postWithObject(TrackingRoutes.postWithActionObject, write(newAction))
    else storedActions :+ newAction
  }


  document.onmousemove = (event: MouseEvent) => {
    var left = 0.0
    var top = 0.0
    if (event.pageX == null && event.clientX != null) {

      left = event.clientX + doc.scrollLeft  - doc.clientLeft
      top = event.clientY + doc.scrollTop - doc.clientTop
    } else {
      left = event.pageX
      top = event.pageY
    }
    mousePosition.left = left
    mousePosition.top = top
  }

  var trackMouse = setInterval(() => track("mm," + mousePosition.left + "," + mousePosition.top), 300)
  if(window.location.href.indexOf("admin") > -1) clearInterval(trackMouse)


  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map{_.asInstanceOf[Html]}.foreach { element =>

        }
    }

}
