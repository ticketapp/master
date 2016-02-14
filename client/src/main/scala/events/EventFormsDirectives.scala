package events


import com.greencatsoft.angularjs.core.Timeout
import com.greencatsoft.angularjs.{TemplatedDirective, ElementDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("eventFormFindById")
class EventFormFindByIdDirective(timeout: Timeout) extends ElementDirective with TemplatedDirective {

  override val templateUrl = "assets/templates/events/eventFormFindById.html"

}