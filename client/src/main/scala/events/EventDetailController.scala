package events

import com.greencatsoft.angularjs.core.Scope
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.scalajs.js
import scala.scalajs.js.native

@injectable("EventDetailCtrl")
class EventDetailController(scope: EventDetailScope) extends AbstractController[EventDetailScope](scope) {

//  scope.event = eventsService.get(cid.toInt)

}

@js.native
trait EventDetailScope extends Scope {
  var event: Happening = native
}