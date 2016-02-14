package tracking

import com.greencatsoft.angularjs.core.{Window, RouteParams, HttpService, Timeout}
import com.greencatsoft.angularjs.{Attributes, ElementDirective, injectable, Angular}
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom.{Event, Element, document, console}
import org.scalajs.dom.html.{Input, Html}
import root.RoutingConfig
import utilities.NgCookies
import scala.scalajs.js.JSON
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.JSExport
import scala.concurrent.ExecutionContext.Implicits.global
import upickle.default._

@JSExport
@injectable("trackingView")
class TrackingViewDirective(timeout: Timeout, httpService: HttpGeneralService, ngCookies: NgCookies,
                            routeParams: RouteParams, window: Window)
  extends ElementDirective {

  var cursor = document.getElementById("cursor").asInstanceOf[Html]
  @JSExport
  var session = Seq.empty[Action]
  @JSExport
  var sessions = Seq.empty[Session].toJSArray

  var trackingViewContainer = document.getElementById("tracking-player").asInstanceOf[Html]
  var trackingViewScroller = trackingViewContainer.getElementsByTagName("md-content").item(0).asInstanceOf[Html]

  window.location.replace(window.location.pathname + "#/")
  timeout( () => {
    val trackedItems = document.getElementsByClassName("tracking")
    for(i <- 0 to(trackedItems.length - 1)) {
      trackedItems.item(i).asInstanceOf[Html].classList.remove("tracking")
    }
    document.onmousemove = (event: Event) => {
    }
  })

  httpService.get(TrackingRoutes.getSessions) map { sessionsString =>
    timeout(() => {
      sessions = read[Seq[Session]](sessionsString).toJSArray
    })
  }

  @JSExport
  def setSession(sessionId: String): Unit = {
    httpService.get(TrackingRoutes.getActionsBySessionId(sessionId)) map { sessionString =>
      session = read[Seq[Action]](sessionString)
    }
  }

  @JSExport
  def play(): Unit = {
    trackingViewContainer.style.width = sessions.filter(_.uuid == session.head.sessionId).head.screenWidth + "px"
    trackingViewContainer.style.height = sessions.filter(_.uuid == session.head.sessionId).head.screenHeight + "px"
    val initTimestamp = session.head.timestamp
    session map { action =>
      console.log(action.action)
      val seqAction = action.action.split(",").toSeq
      val timeToWait: Int = (action.timestamp - initTimestamp).toInt
      timeout(() => {
        trackingViewScroller.scrollTop = seqAction.last.toDouble
        seqAction.headOption match {
          case Some(mouseMove) if mouseMove == "mm" =>
            val leftMousePosition: String = seqAction(1)
            val left = leftMousePosition
            val topMousePosition: String = seqAction(2)
            val top = topMousePosition
            moveCursor(top, left)
          case Some(click) if click == "cl" =>
            val elementId: String = seqAction(1)
            document.getElementById(elementId).asInstanceOf[Html].click()
          case Some(input) if input == "in" =>
            val inputValue: String = seqAction(2)
            document.getElementById(seqAction(1)) match {
              case input1: Input =>
                input1.value = inputValue
              case otherElement => otherElement.asInstanceOf[Html].getElementsByTagName("input").item(0).asInstanceOf[Input].value = inputValue
            }
          case link if link == "a" =>
              val path: String = seqAction(1)
            window.location.replace(window.location.pathname + "#" + path)
        }
      }, timeToWait)
    }
  }

  def moveCursor(top: String, left: String): Unit = {
    cursor.style.top = top + "px"
    cursor.style.left = left + "px"
  }

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
    elements.map{_.asInstanceOf[Html]}.foreach { element =>
      cursor = document.getElementById("cursor").asInstanceOf[Html]
    }
  }

}