package ticketsDomain

import java.util.UUID
import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps


case class Ticket (qrCode: String,
                   eventId: Long,
                   tariffId: Long)

case class TicketStatus(ticketId: Long, status: Char, date: DateTime)

case class TicketWithStatus(ticket: Ticket, ticketStatus: Option[TicketStatus])

case class BlockedTicket(ticketId: Long, expirationDate: DateTime)

case class SellableEvent(eventId: Long)

case class TicketBill(ticketId: Long, userId: UUID, date: DateTime, amount: BigDecimal)

case class PendingTicket(userId: UUID, eventId: Long, date: DateTime, amount: BigDecimal, qrCode: String, isValidated: Option[Boolean] = None)

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

  def addBoughtTicketBill(boughtTicketBill: TicketBill): Future[Int] = db.run(
    boughtTicketBills += boughtTicketBill
  )

  def findBoughtTicketBillByTicketId(ticketId: Long): Future[Seq[TicketBill]] = db.run(
    boughtTicketBills.filter(_.ticketId === ticketId).result
  )

  def addSoldTicketBill(soldTicketBill: TicketBill): Future[Int] = db.run(
    soldTicketBills += soldTicketBill
  )

  def findSoldTicketBillByTicketId(ticketId: Long): Future[Seq[TicketBill]] =  db.run(
    soldTicketBills.filter(_.ticketId === ticketId).result
  )

  def findPendingTickets: Future[Seq[PendingTicket]] = {
    db.run(pendingTickets.result)
  }

  def addPendingTicket(pendingTicket: PendingTicket): Future[Int] = db.run(pendingTickets += pendingTicket)

  def updatePendingTicket(pendingTicket: PendingTicket): Future[Int] = db.run(
    pendingTickets.filter(_.qrCode === pendingTicket.qrCode).filter(_.userId === pendingTicket.userId).update(pendingTicket)
  )
  
  def findUntreatedPendingTickets: Future[Seq[PendingTicket]] = {
      db.run(pendingTickets.filter(_.isValidated.isEmpty).result)
  }

  def findSellableEvents: Future[Seq[SellableEvent]] = db.run(sellableEvents.result)
}
