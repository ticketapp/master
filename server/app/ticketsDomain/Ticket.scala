package ticketsDomain

import javax.inject.Inject
import database.{MyDBTableDefinitions, MyPostgresDriver}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.Controller
import scala.concurrent.Future
import scala.language.postfixOps
import MyPostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class Ticket (qrCode: String,
                   eventId: Long,
                   tariffId: Long)

case class TicketStatus(ticketId: Long, status: Char, date: DateTime)

case class TicketWithStatus(ticket: Ticket, ticketStatus: Option[TicketStatus])

case class BlockedTicket(ticketId: Long, expirationDate: DateTime)

class TicketMethods @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def save(ticket: Ticket): Future[Long] = db.run(tickets returning tickets.map(_.ticketId) += ticket)

  def findAllByEventId(eventId: Long): Future[Seq[TicketWithStatus]] = {
    val query = for {
      ticket <- tickets.filter(_.eventId === eventId)
      ticketStatus <- ticketStatuses if ticketStatus.ticketId === ticket.ticketId
    } yield (ticket, ticketStatus)

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def findAllByTariffId(tariffId: Long): Future[Seq[TicketWithStatus]] = {
    val query = for {
      ticket <- tickets.filter(_.tariffId === tariffId)
      ticketStatus <- ticketStatuses if ticketStatus.ticketId === ticket.ticketId
    } yield (ticket, ticketStatus)

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def findUnblockedByTariffId(tariffId: Long): Future[Seq[TicketWithStatus]] = {
    val query = for {
      ticket <- tickets.filter(_.tariffId === tariffId)
      ticketStatus <- ticketStatuses if ticketStatus.ticketId === ticket.ticketId
    } yield (ticket, ticketStatus)

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }
  
  def addStatus(ticketStatus: TicketStatus): Future[Int] = db.run(ticketStatuses += ticketStatus)

  def maybeTicketAndTicketStatusToOptionTicketStatus(maybeTicketAndStatus: Option[(Ticket, TicketStatus)]):
  Option[TicketStatus] =
    maybeTicketAndStatus match {
      case Some((ticket, ticketStatus)) =>
        Some(ticketStatus)
      case _ =>
        None
    }

  def SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus: Seq[(Ticket, TicketStatus)]):
  Seq[TicketWithStatus] = {
    seqTupleTicketAndStatus.groupBy(_._1) map { ticketAndSeqTicketAndStatus =>
      val ticketAndStatuses = ticketAndSeqTicketAndStatus._2
      val statusSortedByDate = ticketAndStatuses.sortBy(_._2.date.getMillis).reverse
      val mostRecentStatusTuple: Option[(Ticket, TicketStatus)] = statusSortedByDate.take(1).headOption
      val mostRecentStatus = maybeTicketAndTicketStatusToOptionTicketStatus(mostRecentStatusTuple)
      TicketWithStatus(ticketAndSeqTicketAndStatus._1, mostRecentStatus)
    }
  }.toSeq

}
