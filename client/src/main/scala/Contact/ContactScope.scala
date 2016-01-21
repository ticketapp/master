package Contact

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait ContactScope extends Scope {
  var messages: js.Array[Message] = js.native
}