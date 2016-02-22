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


  val eventId = routeParams.get("eventId").get.toString.toInt
  
  service.get(AdminRoutes.findTariffsByEventId(eventId)) map { tariffsFound =>
    timeout(() => sellTicketScope.tariffs = JSON.parse(tariffsFound))
  }
  service.get(EventsRoutes.find(eventId)) map { foundEvent =>
    timeout(() => sellTicketScope.event = JSON.parse(foundEvent))
  }
}