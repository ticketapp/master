package landingPage

import com.greencatsoft.angularjs.core.Scope
import events.Happening

import scala.scalajs.js


@js.native
trait LandingPageScope extends Scope {
  var events: js.Array[Happening] = js.native
}
