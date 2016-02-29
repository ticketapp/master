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
import scala.scalajs.js.{JSON, Date}
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
@injectable("adminController")
class AdminController(adminScope: AdminScope, service: HttpGeneralService, timeout: Timeout, mdToast: MdToastService)
    extends AbstractController[AdminScope](adminScope) with jsonHelper {

  val validationMessage = "Ok"
  var timeBeforeReloadCurrentSessions = 10000

  setInterval(() => {
    service.get(tracking.TrackingRoutes.getCurrentSessions) map { sessions =>
      timeout(() => adminScope.currentSessions = read[Seq[Session]](sessions).toJSArray)
    }
  }, timeBeforeReloadCurrentSessions)


  def findSalableEvents(): Unit = service.get(AdminRoutes.salableEvents) map { foundSalableEvents =>
    timeout(() => adminScope.salableEvents = JSON.parse(foundSalableEvents))
  }

  def findTariffsByEventId(eventId: Int): Future[Dynamic] = {
    service.get(AdminRoutes.findTariffsByEventId(eventId)) map { tariffs =>
      timeout(() => ())
      JSON.parse(tariffs)
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

  def findTicketsWithStatus(): Unit = service.get(AdminRoutes.findTicketsWithStatus) map { ticketsWithStatusFound =>
    timeout(() => adminScope.ticketsWithStatus = JSON.parse(ticketsWithStatusFound))
  }

  def findPendingTickets(): Unit = service.get(AdminRoutes.findPendingTickets) map { pendingTicketsFound =>
    timeout(() => adminScope.pendingTickets = JSON.parse(pendingTicketsFound))
  }

  def findBoughtBills(): Unit = service.get(AdminRoutes.findBoughtBills) map { boughtBillsFind =>
    timeout(() => adminScope.boughtBills = JSON.parse(boughtBillsFind))
  }

  def findSoldBills(): Unit = service.get(AdminRoutes.findSoldBills) map { soldBillsFound =>
    timeout(() => adminScope.soldBills = JSON.parse(soldBillsFound))
  }

  def createSalableEvent(eventId: Int): Unit = service.post(AdminRoutes.salableEvents(eventId: Int)) map { response =>
    val toast = mdToast.simple(validationMessage)
    mdToast.show(toast)
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
