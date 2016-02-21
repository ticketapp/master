package map

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait MapScope extends Scope {
  var zoom: Int = js.native

  var travelMode: String = js.native

  var start: String = js.native
}
