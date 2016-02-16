package admin

import java.util.{UUID, Date}
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Ticket (ticketId: Option[Int],
                   qrCode: String,
                   eventId: Int,
                   tariffId: Int)

@JSExportAll
case class TicketStatus(ticketId: Int, status: Char, date: Date)

@JSExportAll
case class TicketWithStatus(ticket: Ticket, ticketStatus: Option[TicketStatus])

@JSExportAll
case class BlockedTicket(ticketId: Int, expirationDate: Date, userId: UUID)

@JSExportAll
case class SalableEvent(eventId: Int)

@JSExportAll
case class TicketBill(ticketId: Int, userId: UUID, date: Date, amount: Double)

@JSExportAll
case class PendingTicket(pendingTicketId: Option[Int],
                         userId: UUID,
                         tariffId: Int,
                         date: Date,
                         amount: Double,
                         qrCode: String,
                         isValidated: Option[Boolean])

@JSExportAll
case class Tariff (tariffId: Int,
                   denomination: String,
                   eventId: Int,
                   startTime: Date,
                   endTime: Date,
                   price: Double)
