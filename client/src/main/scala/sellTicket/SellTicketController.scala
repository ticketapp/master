package sellTicket

import admin.AdminRoutes
import com.greencatsoft.angularjs.core.{RouteParams, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import events.EventsRoutes
import httpServiceFactory.HttpGeneralService
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("sellTicketController")
class SellTicketController(sellTicketScope: SellTicketScope, httpService: HttpGeneralService, timeout: Timeout,
                           routeParams: RouteParams)
    extends AbstractController[SellTicketScope](sellTicketScope) with jsonHelper {

  val eventId = routeParams.get("eventId").get.toString.toInt

  httpService.get(AdminRoutes.findTariffsByEventId(eventId)) map { tariffsFound =>
    timeout(() => sellTicketScope.tariffs = JSON.parse(tariffsFound))
  }

  httpService.get(EventsRoutes.findById(eventId)) map { foundEvent =>
    timeout(() => sellTicketScope.event = JSON.parse(foundEvent))
  }
}