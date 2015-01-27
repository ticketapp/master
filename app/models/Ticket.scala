package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.Json
import play.api.Play.current
import controllers.DAOException
import java.util.Date

import scala.util.Random

/**
 * Created by sim on 03/10/14.
 */
case class Ticket (ticketId: Long,
                   isValid: Boolean = true,
                   qrCode: String,
                   firstName: String,
                   lastName: String,
                   orderId: Long,
                   tariffId: Long)


object Ticket {
  implicit val ticketWrites = Json.writes[Ticket]


  private val TicketParser: RowParser[Ticket] = {
    get[Long]("ticketId") ~
      get[Boolean]("isValid") ~
      get[String]("qrCode") ~
      get[String]("firstName") ~
      get[String]("lastName") ~
      get[Long]("orderId") ~
      get[Long]("tariffId") map {
      case ticketId ~ isValid ~ qrCode ~ firstName ~ lastName  ~ orderId ~ tariffId=>
        Ticket(ticketId, isValid, qrCode, firstName, lastName, orderId, tariffId)
    }
  }

  def createQrCode(ticketId: Long): String = {
    val rnd = new Random()
    ticketId.toString + (100000.0 + rnd.nextDouble() * (10000000000.0 - 100000.0)).toLong.toString
  }



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
        .on('userId -> user.userId)
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
  }
}
