package events

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait EventScope extends Scope {
  var event: js.Any = js.native
}
