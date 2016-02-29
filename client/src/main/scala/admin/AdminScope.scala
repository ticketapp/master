package admin

import com.greencatsoft.angularjs.core.Scope

import scala.scalajs.js

@js.native
trait AdminScope extends Scope {
  var salableEvents: js.Any = js.native
  var ticketsWithStatus: js.Any = js.native
  var pendingTickets: js.Any = js.native
  var boughtBills: js.Any = js.native
  var soldBills: js.Any = js.native
  var currentSessions: js.Any  = js.native
}
