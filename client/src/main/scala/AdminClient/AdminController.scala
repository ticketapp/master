package AdminClient

import com.greencatsoft.angularjs.core.{Timeout, Scope}
import com.greencatsoft.angularjs.{AbstractController, injectable}
import events.EventsRoutes
import httpServiceFactory.HttpGeneralService
import materialDesign.MdToastService
import upickle.Js
import upickle.Js.Num

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import org.scalajs.dom.console
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExportAll
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import upickle.default._

@JSExportAll
@injectable("adminController")
class AdminController(scope: Scope, service: HttpGeneralService, timeout: Timeout, mdToast: MdToastService)
  extends AbstractController[Scope](scope) {

  implicit val dateTimeWriter = upickle.default.Writer[Date]{
      case t => Js.Str(t.toString)
    }
  implicit val dateTimeReader = upickle.default.Reader[Date]{
      case Js.Str(str) =>
        new Date(str)
        case a =>
        console.log(new Date(a.value.toString))
        //console.log(new Date(write(a.value)))
        new Date(a.value.toString.toLong)
    }

  var salableEvents: js.Array[SalableEvent] = new js.Array[SalableEvent]
  var ticketsWithStatus: js.Array[TicketWithStatus] = new js.Array[TicketWithStatus]
  var pendingTickets: js.Array[PendingTicket] = new js.Array[PendingTicket]
  var boughtBills: js.Array[TicketBill] = new js.Array[TicketBill]
  var soldBills: js.Array[TicketBill] = new js.Array[TicketBill]
  val validationMessage = "Ok"


  def findSalableEvents: Unit = {
    service.getJson(AdminRoutes.salableEvents) map { foundSalableEvents =>
      println(foundSalableEvents)
      timeout(() => salableEvents = read[Seq[SalableEvent]](foundSalableEvents).toJSArray)
    }
  }

  def findTariffsByEventId(eventId: Int): Future[js.Array[Tariff]] = {
    service.getJson(AdminRoutes.findTariffsByEventId(eventId)) map { tariffs =>
      console.log(tariffs)
      timeout( () => read[Seq[Tariff]](tariffs).toJSArray)
      read[Seq[Tariff]](tariffs).toJSArray
    }
  }

  def addTariff(denomination: String, eventId: Int, startTime: Date, endTime: Date, price: Double): Unit = {
    service.postJsonAndRead(AdminRoutes.addTariff(
      denomination: String, eventId: Int, startTime.toISOString(): String, endTime.toISOString(): String, price: Double
    )) map { response =>
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)

    }
  }

  def findTicketsWithStatus: Unit = {
    service.getJson(AdminRoutes.findTicketsWithStatus) map { ticketsWithStatusFound =>
        println(ticketsWithStatusFound)
        timeout(() => ticketsWithStatus = read[Seq[TicketWithStatus]](ticketsWithStatusFound).toJSArray)
    }
  }
  def findPendingTickets: Unit = {
    service.getJson(AdminRoutes.findPendingTickets) map { pendingTicketsFound =>
        println(pendingTicketsFound)
        timeout( () => pendingTickets = read[Seq[PendingTicket]](pendingTicketsFound).toJSArray)
    }
  }
  def findBoughtBills: Unit = {
    service.getJson(AdminRoutes.findBoughtBills) map { boughtBillsFind =>
        println(boughtBillsFind)
        timeout( () => boughtBills = read[Seq[TicketBill]](boughtBillsFind).toJSArray)
    }
  }

  def findSoldBills: Unit = {
    service.getJson(AdminRoutes.findSoldBills) map { soldBillsFound =>
        println(soldBillsFound)
      timeout( () => soldBills = read[Seq[TicketBill]](soldBillsFound).toJSArray)
    }
  }

  def addSalableEvent(eventId: Int): Unit = {
    service.postJsonAndRead(AdminRoutes.salableEvents(eventId: Int)) map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def proposeTicket(tariffId: Int, amount: Double, qrCode: String): Unit = {
    service.postJsonAndRead(AdminRoutes.proposeTicket(tariffId: Int, amount: Double, qrCode: String)) map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def blockTicketForUser(tariffId: Int): Unit = {
    service.postJsonAndRead(AdminRoutes.blockTicketForUser(tariffId: Int)) map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def addTicketToSale(qrCode: String, eventId: Int, tariffId: Int): Unit = {
    service.postJsonAndRead(AdminRoutes.addTicketToSale(qrCode: String, eventId: Int, tariffId: Int))map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def acceptPendingTicket(pendingTicketId: Int): Unit = {
    service.postJsonAndRead(AdminRoutes.acceptPendingTicket(pendingTicketId: Int)) map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }

  def rejectPendingTicket(pendingTicketId: Int): Unit = {
    service.postJsonAndRead(AdminRoutes.rejectPendingTicket(pendingTicketId: Int)) map { response =>
      println(response)
      val toast = mdToast.simple(validationMessage)
      mdToast.show(toast)
    }
  }
}
