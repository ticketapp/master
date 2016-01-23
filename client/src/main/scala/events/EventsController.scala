package events


import com.greencatsoft.angularjs.core.Scope
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.array2JSRichGenTrav

@injectable("EventsCtrl")
class EventsController(scope: EventsScopeType, eventsService: EventsService)
  extends AbstractController[EventsScopeType](scope) {

  println("init EventCtrl")
  scope.events = eventsService.all().toJSArray


}

@js.native
trait EventsScopeType extends Scope {
  var events: js.Array[Happening] = js.native
  var remove: js.Function1[Happening, _] = js.native
}