package events

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait EventsScope extends Scope {
  var testEvents: js.Any = js.native
  var events: js.Array[HappeningWithRelations] = js.native
  var maybeSalableEvents: js.Array[MaybeSalableEvent] = js.native
  var remove: js.Function1[HappeningWithRelations, _] = js.native
}
