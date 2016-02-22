package organizers

import com.greencatsoft.angularjs.core.{RouteParams, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("organizerController")
class OrganizerController(organizerScope: OrganizerScope, organizersService: OrganizersService, routeParams: RouteParams, timeout: Timeout)
  extends AbstractController[OrganizerScope](organizerScope) {

  val organizerId = routeParams.get("id").asInstanceOf[String].toInt

  organizersService.findByIdAsJson(organizerId) map(organizer => timeout(() => organizerScope.organizer = organizer))
}

