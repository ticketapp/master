package adminClient

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
object AdminRoutes {
  def salableEvents: String = "/salableEvents"

  def salableEvents(eventId: Long): String = "/salableEvents?eventId=" + eventId

  def proposeTicket(tariffId: Long, amount: Double, qrCode: String): String =
    "/proposed?tariffId=" + tariffId + "&amount=" + amount + "&qrCode=" + qrCode

  def blockTicketForUser(tariffId: Long): String = "/blockedTicket?tariffId=" + tariffId

  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long): String =
    "/ticketToSale?qrCode=" + qrCode + "&eventId=" + eventId + "&tariffId=" + tariffId

  def acceptPendingTicket(pendingTicketId: Long): String = "/acceptedPendingTicket?pendingTicketId=" + pendingTicketId

  def rejectPendingTicket(pendingTicketId: Long): String = "/rejectedPendingTicket?pendingTicketId=" + pendingTicketId

  def findTicketsWithStatus: String = "/tickets"

  def findPendingTickets: String = "/pending"

  def findBoughtBills: String = "/bills/bought"

  def findSoldBills:String = "/bills/sold "

  def findTariffsByEventId(eventId: Long):String = "/tariffs?eventId=" + eventId

  def addTariff(denomination: String, eventId: Long, startTime: String, endTime: String, price: Double):String =
    "/tariffs?denomination=" + denomination + "&eventId=" + eventId + "&startTime=" + startTime + "&endTime=" +
      endTime + "&price=" + price
}
