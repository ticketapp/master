package events

import com.greencatsoft.angularjs.core.{Timeout, RouteParams}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll
import scala.concurrent.ExecutionContext.Implicits.global

@JSExportAll
@injectable("eventController")
class EventController(eventScope: EventScope, eventsService: EventsService, routeParams: RouteParams, timeout: Timeout)
    extends AbstractController[EventScope](eventScope) {

  val eventId = routeParams.get("id").asInstanceOf[String].toInt

  eventsService.findByIdAsJson(eventId) map(event => timeout(() => eventScope.event = event))
}
