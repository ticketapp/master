package ticketsDomain

import java.util.UUID
import javax.inject.Inject

import com.vividsolutions.jts.geom.Geometry
import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import eventsDomain.Event
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps


case class Ticket (ticketId: Option[Long] = None,
                   qrCode: String,
                   eventId: Long,
                   tariffId: Long)

case class TicketStatus(ticketId: Long, status: Char, date: DateTime)

case class TicketWithStatus(ticket: Ticket, ticketStatus: Option[TicketStatus])

case class BlockedTicket(ticketId: Long, expirationDate: DateTime, userId: UUID)

case class SalableEvent(eventId: Long)

case class MaybeSalableEvent(event: Event, isSalable: Boolean)

case class TicketBill(ticketId: Long, userId: UUID, date: DateTime, amount: BigDecimal)

case class PendingTicket(pendingTicketId: Option[Long],
                         userId: UUID,
                         tariffId: Long,
                         date: DateTime,
                         amount: BigDecimal,
                         qrCode: String,
                         isValidated: Option[Boolean] = None)

class TicketMethods @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[MyPostgresDriver] with MyDBTableDefinitions {

  def save(ticket: Ticket): Future[Long] = db.run(tickets returning tickets.map(_.ticketId) += ticket)

  def findAll(): Future[Seq[TicketWithStatus]] = {
    val query = for {
      ticket <- tickets joinLeft ticketStatuses on (_.ticketId === _.ticketId)
    } yield ticket

    db.run(query.result) map { seqTupleTicketAndStatus =>
      SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus)
    }
  }

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

    db.run(query.result) map(seqTupleTicketAndStatus => SeqTupleTicketAndStatusToSeqTicketWithStatus(seqTupleTicketAndStatus))
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

  def blockTicket(durationInSecond: Int, ticketId: Long, userId: UUID): Future[Int] = db.run(
    blockedTickets += BlockedTicket(ticketId = ticketId, expirationDate = new DateTime().plusSeconds(durationInSecond), userId)
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

  def addBoughtTicketBill(boughtTicketBill: TicketBill): Future[Int] =
    db.run(boughtTicketBills += boughtTicketBill)

  def findBoughtTicketBillByTicketId(ticketId: Long): Future[Seq[TicketBill]] = db.run(
    boughtTicketBills.filter(_.ticketId === ticketId).result
  )

  def findAllBoughtTicketBill: Future[Seq[TicketBill]] = db.run(boughtTicketBills.result)

  def findAllSoldTicketBill: Future[Seq[TicketBill]] = db.run(soldTicketBills.result)

  def addSoldTicketBill(soldTicketBill: TicketBill): Future[Int] = db.run(
    soldTicketBills += soldTicketBill
  )

  def findSoldTicketBillByTicketId(ticketId: Long): Future[Seq[TicketBill]] =  db.run(
    soldTicketBills.filter(_.ticketId === ticketId).result
  )

  def findPendingTickets: Future[Seq[PendingTicket]] = db.run(pendingTickets.result)

  def findPendingTicketById(pendingTicketId:Long): Future[Option[PendingTicket]] =
    db.run(pendingTickets.filter(_.pendingTicketId === pendingTicketId).result) map (_.headOption)

  def addPendingTicket(pendingTicket: PendingTicket): Future[Int] = db.run(pendingTickets += pendingTicket)

  def updatePendingTicket(pendingTicketId: Option[Long], isValidate: Boolean): Future[Int] = {
    val q = for { c <- pendingTickets if c.pendingTicketId === pendingTicketId } yield c.isValidated
    db.run(q.update(Some(isValidate)))
  }
  
  def findUntreatedPendingTickets: Future[Seq[PendingTicket]] = db.run(pendingTickets.filter(_.isValidated.isEmpty).result)

  def findSalableEvents: Future[Seq[SalableEvent]] = db.run(salableEvents.result)

  def findMaybeSalableEventsContaining(pattern: String): Future[Seq[MaybeSalableEvent]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val lowercasePattern = pattern.toLowerCase
    val query = for {
      event <- events.filter(event =>
          (event.name.toLowerCase like s"%$lowercasePattern%") &&
            ((event.endTime.nonEmpty && event.endTime > now) || (event.endTime.isEmpty && event.startTime > twelveHoursAgo)))
        .sortBy(event => (event.startTime.desc, event.id))
    } yield event

    db.run(query.result) flatMap { events =>
      Future.sequence(
        events map { event =>
          val isSalableEvent = db.run(salableEvents.filter(_.eventId === event.id.get).result)
          isSalableEvent map { salableEvents =>
            salableEvents.headOption match {
              case Some(salableEvent) =>
                MaybeSalableEvent(event, true)
              case _ =>
                MaybeSalableEvent(event, false)
            }
          }
        }
      )
    }
  }

  def findMaybeSalableEventsNear(geographicPoint: Geometry, offset: Int, numberToReturn: Int): Future[Seq[MaybeSalableEvent]] = {
    val now = DateTime.now()
    val twelveHoursAgo = now.minusHours(12)
    val query = for {
      event <- (salableEvents join events on(_.eventId === _.id)).filter { salableEventWithEvent =>
        val eventFound = salableEventWithEvent._2
        (eventFound.endTime.nonEmpty && eventFound.endTime > now) ||
          (eventFound.endTime.isEmpty && eventFound.startTime > twelveHoursAgo)
      }
      .sortBy(event => (event._2.geographicPoint <-> geographicPoint, event._2.id))
      .drop(offset)
      .take(numberToReturn)
    } yield event._2

    db.run(query.result) map { events =>
      events map (event => MaybeSalableEvent(event, true))
    }
  }

  def addSalableEvents(salableEvent: SalableEvent): Future[Int] = db.run(salableEvents += salableEvent)
}
