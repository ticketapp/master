package places

import com.greencatsoft.angularjs.core.{RouteParams, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("placeController")
class PlaceController(placeScope: PlaceScope, placesService: PlacesService, routeParams: RouteParams, timeout: Timeout)
    extends AbstractController[PlaceScope](placeScope) {

  val placeId = routeParams.get("id").asInstanceOf[String].toInt

  placesService.findByIdAsJson(placeId) map(place => timeout(() => placeScope.place = place))
}
