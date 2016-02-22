package events

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait EventsScope extends Scope {
  var events: js.Any = js.native
  var maybeSalableEvents: js.Any = js.native
  var remove: js.Function1[HappeningWithRelations, _] = js.native
}
