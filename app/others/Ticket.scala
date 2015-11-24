package others

import play.api.libs.json.Json

import scala.language.postfixOps
import scala.util.Random

case class Ticket (ticketId: Long,
                   isValid: Boolean = true,
                   qrCode: String,
                   firstName: String,
                   lastName: String,
                   orderId: Long,
                   tariffId: Long)


object Ticket {
  implicit val ticketWrites = Json.writes[Ticket]

  def createQrCode(ticketId: Long): String = {
    val rnd = new Random()
    ticketId.toString + (100000.0 + rnd.nextDouble() * (10000000000.0 - 100000.0)).toLong.toString
  }


/*
  def findAll(): Seq[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL("select * from tickets").as(TicketParser *)
    }
  }

  def find(ticketId: Long): Option[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * from tickets WHERE ticketId = {ticketId}")
        .on('ticketId -> ticketId)
        .as(TicketParser.singleOpt)
    }
  }

  def findAllByOrder(order: Order): Seq[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Tickets
             WHERE orderId = {orderId}""")
        .on('orderId -> order.orderId)
        .as(TicketParser *)
    }
  }

  def findAllByTariff(tariff: Tariff): Seq[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Tickets
             WHERE tariffId = {tariffId}""")
        .on('tariffId -> tariff.tariffId)
        .as(TicketParser *)
    }
  }

  def findAllByEvent(event: Event): Seq[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
             FROM Tickets
             WHERE eventId = {eventId}""")
        .on('eventId -> event.eventId)
        .as(TicketParser *)
    }
  }

  def findAllByUser(user: User): Seq[Ticket] = {
    DB.withConnection { implicit connection =>
      SQL( """SELECT *
           FROM Tickets
           WHERE userId = {userId}""")
        .on('userId -> user.UUID)
        .as(TicketParser *)
    }
  }
  
  def save(ticket: Ticket) = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """INSERT INTO tickets(qrCode orderId, tariffId)
            VALUES({qrCode}, {denomination}, {nbTicketToSell})
          """).on(
            'qrCode -> ticket.qrCode,
            'orderId -> ticket.orderId,
            'tariffId -> ticket.tariffId
          ).executeInsert().get
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot save ticket: " + e.getMessage)
    }
  }*/
}