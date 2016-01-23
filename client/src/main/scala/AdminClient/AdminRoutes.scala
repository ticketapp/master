package AdminClient

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
object AdminRoutes {
  def salableEvents: String = "/salableEvents"
  def salableEvents(eventId: Long): String = "/salableEvents?eventId=" + eventId
  def proposeTicket(tariffId: Long, amount: Double, qrCode: String): String =
    "/tickets/propose?tariffId=" + tariffId + "&amount=" + amount + "&qrCode=" + qrCode
  def blockTicketForUser(tariffId: Long): String = "/tickets/blockForUser?tariffId=" + tariffId
  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long): String =
    "/tickets/addTicketToSale?qrCode=" + qrCode + "&eventId=" + eventId + "&tariffId=" + tariffId
  def acceptPendingTicket(pendingTicketId: Long): String = "/tickets/propose?pendingTicketId=" + pendingTicketId
  def rejectPendingTicket(pendingTicketId: Long): String = "/tickets/rejectPenddingTicket?pendingTicketId=" + pendingTicketId
  def findTicketsWithStatus: String = "/tickets/findAll"
  def findPendingTickets: String = "/tickets/pending"
  def findBoughtBills: String = "/bills/bought"
  def findSoldBills:String = "/bills/sold "
  def findTariffsByEventId(eventId: Long):String = "/tariffs?eventId=" + eventId
  def addTariff(denomination: String, eventId: Long, startTime: String, endTime: String, price: Double):String =
    "/tariffs?denomination=" + denomination + "&eventId=" + eventId + "&startTime=" + startTime + "&endTime=" +
      endTime + "&price=" + price
}
