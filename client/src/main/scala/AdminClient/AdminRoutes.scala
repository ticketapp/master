package AdminClient

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
trait AdminRoutes extends js.Object {
  def salableEvents: String = "/salableEvents"
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
  def findSoldBills:String = "/bills/sold "
}