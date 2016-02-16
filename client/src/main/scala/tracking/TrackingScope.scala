package tracking

import com.greencatsoft.angularjs.core.Scope
import httpServiceFactory.HttpGeneralService
import org.scalajs.dom.html._

import scala.scalajs.js

@js.native
trait TrackingScope extends Scope {
  var sessionId: String = js.native
  var storedActions: Seq[Action] = js.native
  var doc: Html = js.native
  var httpService: HttpGeneralService = js.native
}