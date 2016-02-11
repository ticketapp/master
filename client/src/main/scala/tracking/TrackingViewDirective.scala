package tracking

import com.greencatsoft.angularjs.core.{RouteParams, HttpService, Timeout}
import com.greencatsoft.angularjs.{Attributes, ElementDirective, injectable, Angular}
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom.Element
import org.scalajs.dom.document
import org.scalajs.dom.html.{Input, Html}
import org.scalajs.dom.console
import root.RoutingConfig
import utilities.NgCookies
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExport
import scala.concurrent.ExecutionContext.Implicits.global
import upickle.default._

@JSExport
@injectable("trackingView")
class TrackingViewDirective(timeout: Timeout, httpService: HttpGeneralService, ngCookies: NgCookies, routeParams: RouteParams)
  extends ElementDirective {

  var cursor = document.getElementById("cursor").asInstanceOf[Html]
  /*val action1 = Action("mm,20,20", 100.0, 1)
  val action2 = Action("mm,30,30", 600.0, 1)
  val action3 = Action("mm,40,30", 1000.0, 1)
  val action4 = Action("mm,50,30", 1500.0, 1)
//  val action5 = Action("cl,b1", 2000.0, 1)
  val action6 = Action("in,li1,a", 2000.0, 1)
  val action7 = Action("in,li1,an", 2100.0, 1)
  val action8 = Action("a,/events", 2100.0, 1)*/

  var sessions = Seq.empty[Action]

  @JSExport
  var template = "assets/templates/landingPage/landingPage.html"

  httpService.get(TrackingRoutes.getActionsBySessionId(ngCookies.get("sessionId").toString)) map { sessionsString =>
    sessions = read[Seq[Action]](sessionsString)
  }

  @JSExport
  def play(): Unit = {
    val initTimestamp = sessions.head.timestamp.toInt
    sessions map { action =>
      val seqAction = action.action.split(",").toSeq
      timeout( () => {
        seqAction.head match {
          case mouseMouve if mouseMouve == "mm" =>
            val left = seqAction(1)
            val top = seqAction(2)
            mouveCursor(top, left)
          case click if click == "cl" =>
            document.getElementById(seqAction(1)).asInstanceOf[Html].click()
          case input if input == "in" =>
            document.getElementById(seqAction(1)).asInstanceOf[Input].value = seqAction(2)
          case link if link == "a" =>
            template = RoutingConfig.urlTemplatePath(seqAction(1))
        }
        }, action.timestamp.toInt - initTimestamp
      )
    }
  }

  def mouveCursor(top: String, left: String): Unit = {
    cursor.style.top = top + "px"
    cursor.style.left = left + "px"
  }

  override def link(scopeType: ScopeType, elements: Seq[Element], attributes: Attributes): Unit = {
        elements.map{_.asInstanceOf[Html]}.foreach { element =>
          cursor = document.getElementById("cursor").asInstanceOf[Html]
        }
    }

}