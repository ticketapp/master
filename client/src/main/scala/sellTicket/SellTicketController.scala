package sellTicket

import admin.AdminRoutes
import com.greencatsoft.angularjs.core.{RouteParams, Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import events.EventsRoutes
import geolocation.GeolocationService
import httpServiceFactory.HttpGeneralService
import utilities.jsonHelper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("sellTicketController")
class SellTicketController(sellTicketScope: SellTicketScope, service: HttpGeneralService, timeout: Timeout,
                       geolocationService: GeolocationService, routeParams: RouteParams)
  extends AbstractController[SellTicketScope](sellTicketScope) with jsonHelper {


  val maybeEventId = routeParams.get("id")

  maybeEventId match {

    case eventId if eventId.isDefined =>
      val id = eventId.get.toString.toInt
      service.get(AdminRoutes.findTariffsByEventId(id)) map { tariffsFound =>
        timeout(() => sellTicketScope.tariffs = JSON.parse(tariffsFound))
      } recover { case t: Throwable =>
        sellTicketScope.message = "Error on found tariffs"
      }

      service.get(EventsRoutes.find(id)) map {

        case foundEvent if foundEvent.length > 0 =>
          timeout(() => sellTicketScope.event = JSON.parse(foundEvent))

        case _ =>
          sellTicketScope.message = "no event found"
      } recover { case t: Throwable =>
        sellTicketScope.message = "Error on found event"
      }

    case _ =>
      sellTicketScope.message = "url error"
  }
}

trait SellTicketScope extends Scope {
  var message: js.Any = js.native
  var event: js.Any = js.native
  var tariffs: js.Any = js.native
}