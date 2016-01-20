package ticketsDomain

import javax.inject.Inject
import database.{MyDBTableDefinitions, MyPostgresDriver}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import org.joda.time.{Duration, DateTime}
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
      ticket <- tickets joinLeft ticketStatuses on (_.ticketId === _.ticketId)
      if ticket._1.eventId === eventId
    } yield ticket

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def findAllByTariffId(tariffId: Long): Future[Seq[TicketWithStatus]] = {
    val query = for {
      ticket <- tickets joinLeft ticketStatuses on (_.ticketId === _.ticketId)
      if ticket._1.tariffId === tariffId
    } yield ticket

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def findUnblockedByTariffId(tariffId: Long): Future[Seq[TicketWithStatus]] = {
    val currentDateTime = new DateTime()
    val blockedTicketIds = blockedTickets.filter(_.expirationDate >= currentDateTime).map(_.ticketId)

    val query = for {
      ticket <- tickets.filterNot(_.ticketId in blockedTicketIds) joinLeft ticketStatuses on (_.ticketId === _.ticketId)
      if ticket._1.tariffId === tariffId

    } yield ticket

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def findUnblockedByEventId(eventId: Long): Future[Seq[TicketWithStatus]] = {
    val currentDateTime = new DateTime()
    val blockedTicketIds = blockedTickets.filter(_.expirationDate >= currentDateTime).map(_.ticketId)

    val query = for {
      ticket <- tickets.filterNot(_.ticketId in blockedTicketIds) joinLeft ticketStatuses on (_.ticketId === _.ticketId)
      if ticket._1.eventId === eventId

    } yield ticket

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

  def blockTicket(duration: Int, ticketId: Long): Future[Int] = db.run(
    blockedTickets += BlockedTicket(ticketId = ticketId, expirationDate = new DateTime().plusSeconds(duration))
  )
  
  def addStatus(ticketStatus: TicketStatus): Future[Int] = db.run(ticketStatuses += ticketStatus)

  def SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus: Seq[(Ticket, Option[TicketStatus])]):
  Seq[TicketWithStatus] = {
    seqTupleTicketAndStatus.groupBy(_._1) map { ticketAndSeqTicketAndStatus =>
      val ticketAndStatuses = ticketAndSeqTicketAndStatus._2
      val ticketsStatus = ticketAndStatuses collect {
        case (_, Some(ticketStatus)) => ticketStatus
      }
      val statusSortedByDate = ticketsStatus.sortBy(_.date.getMillis).reverse
      val mostRecentStatus: Option[TicketStatus] = statusSortedByDate.take(1).headOption
      TicketWithStatus(ticketAndSeqTicketAndStatus._1, mostRecentStatus)
    }
  }.toSeq

}
