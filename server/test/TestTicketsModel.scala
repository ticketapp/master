import testsHelper.GlobalApplicationForModels
import ticketsDomain.{TicketStatus, TicketWithStatus, Ticket}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import org.joda.time.DateTime

class TestTicketsModel extends GlobalApplicationForModels {

  "A ticket" must {

    val savedTicket:Ticket = Ticket("savedTicket",100,10000)
    val savedBlockedTicket:Ticket = Ticket("savedBlockedTicket",100,10000)
    val oldSavedStatus = TicketStatus(1000,'a',new DateTime("2015-09-22T14:00:00.000+02:00"))
    val newSavedStatus = TicketStatus(1000,'b',new DateTime("2015-09-24T14:00:00.000+02:00"))

    "return id on save" in {
      val ticket = Ticket(qrCode = "test", eventId = 100, tariffId = 10000)
      whenReady(ticketMethods.save(ticket)) { ticketId =>
        ticketId mustBe 1
      }
    }

    "pass a Seq of Tuple of Ticket And Status to Seq of TicketWithStatus with the most recent status" in {
      val newTicket = Ticket("newTicket", 100, 10000)
      val tupleTicketAndStatus1 = (savedTicket, oldSavedStatus)
      val tupleTicketAndStatus2 = (savedTicket, newSavedStatus)
      val seqOfTicketWithStatus = ticketMethods.SeqTupleTicketAndStatusToSeqTicketWithStatus(
        Seq(tupleTicketAndStatus1, tupleTicketAndStatus2)
      )
      seqOfTicketWithStatus mustBe Seq(TicketWithStatus(savedTicket, Some(newSavedStatus)))
    }

    "find all tickets by eventId" in {
      whenReady(ticketMethods.findAllByEventId(100)) { tickets =>
        tickets must contain (TicketWithStatus(savedTicket, Some(newSavedStatus)))
        tickets must contain (TicketWithStatus(savedBlockedTicket, None))
        tickets must not contain TicketWithStatus(savedTicket, Some(oldSavedStatus))
      }
    }

    "find all tickets by tariffId" in {
      whenReady(ticketMethods.findAllByTariffId(10000)) { tickets =>
        tickets must contain (TicketWithStatus(savedTicket, Some(newSavedStatus)))
        tickets must contain (TicketWithStatus(savedBlockedTicket, None))
        tickets must not contain TicketWithStatus(savedTicket, Some(oldSavedStatus))
      }
    }

    "add status for a ticket" in {
      val newStatus = TicketStatus(1000,'c',new DateTime())
      whenReady(ticketMethods.addStatus(newStatus)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findAllByEventId(100)){ tickets =>
          tickets must contain (TicketWithStatus(savedTicket, Some(newStatus)))
          tickets must not contain TicketWithStatus(savedTicket, Some(newSavedStatus))
          tickets must not contain TicketWithStatus(savedTicket, Some(oldSavedStatus))
        }
      }
    }

    "find only unblocked tickets by tariffId" in {
        whenReady(ticketMethods.findUnblockedByTariffId(10000)) { unblockedTickets =>
          unblockedTickets mustBe Seq.empty
        }
    }
  }
}