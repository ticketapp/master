package AdminClient

import java.util.UUID

import com.greencatsoft.angularjs.core.Scope
import com.greencatsoft.angularjs.{AbstractController, injectable}
import httpServiceFactory.HttpGeneralService

import scala.concurrent.Future
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExportAll
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

@JSExportAll
case class Ticket ( ticketId: Option[Long] = None,
                    qrCode: String,
                    eventId: Long,
                    tariffId: Long)

case class TicketStatus(ticketId: Long, status: Char, date: Date)

case class TicketWithStatus(ticket: Ticket, ticketStatus: Option[TicketStatus])

case class BlockedTicket(ticketId: Long, expirationDate: Date, userId: UUID)


sealed trait A
case class SalableEvent(eventId: Long) extends A

case class TicketBill(ticketId: Long, userId: UUID, date: Date, amount: BigDecimal)

case class PendingTicket(pendingTicketId: Option[Long],
                         userId: UUID,
                         tariffId: Long,
                         date: Date,
                         amount: BigDecimal,
                         qrCode: String,
                         isValidated: Option[Boolean] = None)

@injectable("contactController")
class AdminController(scope: Scope, httpService: HttpGeneralService, adminRoutes: AdminRoutes)
  extends AbstractController[Scope](scope) {

  def getSalableEvents: Future[Seq[A]] = {
    httpService.getJsonAndRead(adminRoutes.salableEvents) map { salableEvents =>
        println(salableEvents)
        salableEvents
    } recover { case t: Throwable =>
        println(throw t)
        Seq.empty
    }
  }
}


/*def salableEvents: String = "/salableEvents"
  def proposeTicket(eventId: Long, amount: Double, qrCode: String): String =
    "/tickets/propose?eventId=" + eventId + "&amount=" + amount + "&qrCode=" + qrCode
  def blockTicketForUser(tariffId: Long): String = "/tickets/blockForUser?tariffId=" + tariffId
  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long): String =
    "/tickets/addTicketToSale?qrCode=" + qrCode + "&eventId=" + eventId + "&tariffId=" + tariffId
  def acceptPendingTicket(pendingTicketId: Long): String = "/tickets/propose?pendingTicketId=" + pendingTicketId
  def rejectPendingTicket(pendingTicketId: Long): String = "/tickets/rejectPenddingTicket?pendingTicketId=" + pendingTicketId
  def findTicketsWithStatus: String = "/tickets/findAll"
  def findPendingTickets: String = "/tickets/pending"
  def findBoughtBills: String = "/bills/bought"
  def findSoldBills:String = "/bills/sold "*/