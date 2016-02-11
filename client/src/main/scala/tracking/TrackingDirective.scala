package tracking


import java.util.UUID

import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs._
import com.greencatsoft.angularjs
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom._
import org.scalajs.dom.html.Html
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
class TrackingDirective(timeout: Timeout, ngCookies: NgCookies, httpService: HttpGeneralService) extends AttributeDirective {

  val storedActions: Seq[Action] = Seq.empty[Action]
  var sessionId = ""
  val cookie = ngCookies.get("sessionId") match {
    case string if string.isInstanceOf[String] =>
      sessionId = string.asInstanceOf[String]
    case _ =>
      httpService.post(TrackingRoutes.postSession) map { newSessionId =>
        ngCookies.put("sessionId", newSessionId)
        sessionId = newSessionId
        storedActions map { action =>
          httpService.postWithObject(TrackingRoutes.postWithActionObject, write(action))
        }

      }
  }

  @JSExport
  def track(action: String): Unit = {
    val newAction = Action(action, new Date().getDate(), sessionId)
    if(sessionId.length > 0) httpService.postWithObject(TrackingRoutes.postWithActionObject, write(newAction))
    else storedActions :+ newAction
  }

  var mousePosition = new Object().asInstanceOf[MousePosition]
  document.addEventListener("onmousemove", handleMouseMove)
  var trackMouse = setInterval(() => track("mm," + mousePosition.left + "," + mousePosition.top), 300)
  val handleMouseMove = (event: MouseEvent) => {
    var left = event.pageX
    var top = event.pageY
    if (event.pageX == null && event.clientX != null) {
      val doc = document.getElementsByTagName("md-content").item(0).asInstanceOf[Html]
      left = event.clientX + doc.scrollLeft  - doc.clientLeft
      top = event.clientY + doc.scrollTop - doc.clientTop
    }
    mousePosition.left = left
    mousePosition.top = top
  }

//  var trackMouse = setInterval(() => track("mm," + ))
/* var mousePos;

    document.onmousemove = handleMouseMove;
    setInterval(getMousePosition, 100); // setInterval repeats every X ms

    function handleMouseMove(event) {
        var dot, eventDoc, doc, body, pageX, pageY;

        event = event || window.event; // IE-ism

        // If pageX/Y aren't available and clientX/Y are,
        // calculate pageX/Y - logic taken from jQuery.
        // (This is to support old IE)
        if (event.pageX == null && event.clientX != null) {
            eventDoc = (event.target && event.target.ownerDocument) || document;
            doc = eventDoc.documentElement;
            body = eventDoc.body;

            event.pageX = event.clientX +
              (doc && doc.scrollLeft || body && body.scrollLeft || 0) -
              (doc && doc.clientLeft || body && body.clientLeft || 0);
            event.pageY = event.clientY +
              (doc && doc.scrollTop  || body && body.scrollTop  || 0) -
              (doc && doc.clientTop  || body && body.clientTop  || 0 );
        }

        mousePos = {
            x: event.pageX,
            y: event.pageY
        };
    }
    function getMousePosition() {
        var pos = mousePos;
        if (!pos) {
            // We haven't seen any movement yet
        }
        else {
            // Use pos.x and pos.y
        }
    }*/
  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map{_.asInstanceOf[Html]}.foreach { element =>

        }
    }

}
