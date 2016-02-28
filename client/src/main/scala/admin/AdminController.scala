package admin

import com.greencatsoft.angularjs.core.{Scope, Timeout}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService
import org.scalajs.dom.setInterval
import tracking.Session
import upickle.default._
import utilities.jsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("adminController")
class AdminController(scope: Scope, service: HttpGeneralService, timeout: Timeout, mdToast: MdToastService)
    extends AbstractController[Scope](scope) with jsonHelper {

  var salableEvents: js.Array[SalableEvent] = new js.Array[SalableEvent]
  var ticketsWithStatus: js.Array[TicketWithStatus] = new js.Array[TicketWithStatus]
  var pendingTickets: js.Array[PendingTicket] = new js.Array[PendingTicket]
  var boughtBills: js.Array[TicketBill] = new js.Array[TicketBill]
  var soldBills: js.Array[TicketBill] = new js.Array[TicketBill]
  val validationMessage = "Ok"
  var currentSessions = new js.Array[Session]()
  var timeBeforeReloadCurrentSessions = 10000
  setInterval(() => {
    service.get(tracking.TrackingRoutes.getCurrentSessions) map { sessions =>
      timeout(() => currentSessions = read[Seq[Session]](sessions).toJSArray)
    }
  }, timeBeforeReloadCurrentSessions)


  def findSalableEvents(): Unit = {
    service.get(AdminRoutes.salableEvents) map { foundSalableEvents =>
      timeout(() => salableEvents = read[Seq[SalableEvent]](foundSalableEvents).toJSArray)
    }
  }

  def findTariffsByEventId(eventId: Int): Future[js.Array[Tariff]] = {
    service.get(AdminRoutes.findTariffsByEventId(eventId)) map { tariffs =>
      timeout( () => read[Seq[Tariff]](tariffs).toJSArray)
      read[Seq[Tariff]](tariffs).toJSArray
    }
  }

  def createTariff(denomination: String, eventId: Int, startTime: Date, endTime: Date, price: Double): Unit = {
    service.post(AdminRoutes.addTariff(
      denomination: String, eventId: Int, startTime.toISOString(): String, endTime.toISOString(): String, price: Double
    )) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def findTicketsWithStatus(): Unit = {
    service.get(AdminRoutes.findTicketsWithStatus) map { ticketsWithStatusFound =>
        timeout(() => ticketsWithStatus = read[Seq[TicketWithStatus]](ticketsWithStatusFound).toJSArray)
    }
  }

  def findPendingTickets(): Unit = {
    service.get(AdminRoutes.findPendingTickets) map { pendingTicketsFound =>
        timeout(() => pendingTickets = read[Seq[PendingTicket]](pendingTicketsFound).toJSArray)
    }
  }

  def findBoughtBills(): Unit = {
    service.get(AdminRoutes.findBoughtBills) map { boughtBillsFind =>
        timeout(() => boughtBills = read[Seq[TicketBill]](boughtBillsFind).toJSArray)
    }
  }

  def findSoldBills(): Unit = {
    service.get(AdminRoutes.findSoldBills) map { soldBillsFound =>
      timeout(() => soldBills = read[Seq[TicketBill]](soldBillsFound).toJSArray)
    }
  }

  def createSalableEvent(eventId: Int): Unit = {
    service.post(AdminRoutes.salableEvents(eventId: Int)) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def proposeTicket(tariffId: Int, amount: Double, qrCode: String): Unit = {
    service.post(AdminRoutes.proposeTicket(tariffId: Int, amount: Double, qrCode: String)) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def blockTicketForUser(tariffId: Int): Unit = {
    service.post(AdminRoutes.blockTicketForUser(tariffId: Int)) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def createTicketToSale(qrCode: String, eventId: Int, tariffId: Int): Unit = {
    service.post(AdminRoutes.addTicketToSale(qrCode: String, eventId: Int, tariffId: Int))map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def acceptPendingTicket(pendingTicketId: Int): Unit = {
    service.post(AdminRoutes.acceptPendingTicket(pendingTicketId: Int)) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def rejectPendingTicket(pendingTicketId: Int): Unit = {
    service.post(AdminRoutes.rejectPendingTicket(pendingTicketId: Int)) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }
}
