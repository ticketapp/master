package events

import com.greencatsoft.angularjs.{TemplatedDirective, ElementDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("eventAndPastEventsTabs")
class EventAndPastEventsTabsDirective() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventAndPastEventsTabs.html"
}
