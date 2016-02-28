package chatContact

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait AdminChatContactScope extends Scope {
  var messages: js.Array[Message] = js.native
}
