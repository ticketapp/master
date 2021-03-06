package admin

object AdminRoutes {
  def salableEvents: String = "/salableEvents"

  def salableEvents(eventId: Long): String = "/salableEvents?eventId=" + eventId

  def proposeTicket(tariffId: Long, amount: Double, qrCode: String): String =
    "/proposedTickets?tariffId=" + tariffId + "&amount=" + amount + "&qrCode=" + qrCode

  def blockTicketForUser(tariffId: Long): String = "/blockedTickets?tariffId=" + tariffId

  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long): String =
    "/ticketsToSale?qrCode=" + qrCode + "&eventId=" + eventId + "&tariffId=" + tariffId

  def acceptPendingTicket(pendingTicketId: Long): String = "/acceptedPendingTickets?pendingTicketId=" + pendingTicketId

  def rejectPendingTicket(pendingTicketId: Long): String = "/rejectedPendingTickets?pendingTicketId=" + pendingTicketId

  def findTicketsWithStatus: String = "/tickets"

  def findPendingTickets: String = "/pendingTickets"

  def findBoughtBills: String = "/boughtBills"

  def findSoldBills: String = "/soldBills"

  def findTariffsByEventId(eventId: Long): String = "/tariffs?eventId=" + eventId

  def addTariff(denomination: String, eventId: Long, startTime: String, endTime: String, price: Double): String =
    "/tariffs?denomination=" + denomination + "&eventId=" + eventId + "&startTime=" + startTime + "&endTime=" +
      endTime + "&price=" + price
}
