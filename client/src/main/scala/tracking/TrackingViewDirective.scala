package tracking

import com.greencatsoft.angularjs.core.{RouteParams, Timeout, Window}
import com.greencatsoft.angularjs.{Attributes, ElementDirective, injectable}
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom.html.{Html, Input}
import org.scalajs.dom.{Element, document}
import upickle.default._
import utilities.NgCookies

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("trackingView")
class TrackingViewDirective(timeout: Timeout, httpService: HttpGeneralService, ngCookies: NgCookies,
                            routeParams: RouteParams, window: Window) extends ElementDirective {

  var cursor = document.getElementById("cursor").asInstanceOf[Html]
  @JSExport
  var session = Seq.empty[Action]

  @JSExport
  var sessions = Seq.empty[Session].toJSArray

  var trackingViewContainer = document.getElementById("tracking-player").asInstanceOf[Html]
  var trackingViewScroller = trackingViewContainer.getElementsByTagName("md-content").item(0).asInstanceOf[Html]

  if(window.location.hash.length > 0) window.location.hash = ""
  window.location.replace(window.location.pathname + "#/")
  timeout(() => {
    val trackedItems = document.getElementsByClassName("tracking")
    for(i <- 0 until trackedItems.length) {
      trackedItems.item(i).asInstanceOf[Html].classList.remove("tracking")
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
              case otherElement =>
                otherElement.asInstanceOf[Html].getElementsByTagName("input").item(0).asInstanceOf[Input].value = inputValue
            }

          case Some(link) if link == "a" =>
              val path: String = seqAction(1)
            window.location.replace(window.location.pathname + "#" + path)

          case _ =>

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