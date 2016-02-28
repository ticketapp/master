package events

import com.greencatsoft.angularjs.core.{RouteParams, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("eventController")
class EventController(eventScope: EventScope, eventsService: EventsService, routeParams: RouteParams, timeout: Timeout)
    extends AbstractController[EventScope](eventScope) {

  val eventId = routeParams.get("id").asInstanceOf[String].toInt

  eventsService.findByIdAsJson(eventId) map(event => timeout(() => eventScope.event = event))
}
