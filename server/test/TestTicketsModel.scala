import java.util.UUID

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import testsHelper.GlobalApplicationForModels
import ticketsDomain._

import scala.language.postfixOps

class TestTicketsModel extends GlobalApplicationForModels {

  "A ticket" must {

    val savedTicket:Ticket = Ticket("savedTicket",100,10000)
    val savedBlockedTicket:Ticket = Ticket("savedBlockedTicket",100,10000)
    val oldSavedStatus = TicketStatus(1000,'a',new DateTime("2015-09-22T14:00:00.000+02:00"))
    val newSavedStatus = TicketStatus(1000,'b',new DateTime("2015-09-24T14:00:00.000+02:00"))
    val savedPendingTicket = PendingTicket(
      userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
      eventId = 100,
      date = new DateTime("2015-09-24T14:00:00.000+02:00"),
      amount = 10,
      qrCode = "pendingTicket"
    )

    "return id on save" in {
      val ticket = Ticket(qrCode = "test", eventId = 100, tariffId = 10000)
      whenReady(ticketMethods.save(ticket)) { ticketId =>
        ticketId mustBe 1
      }
    }

    "pass a Seq of Tuple of Ticket And Status to Seq of TicketWithStatus with the most recent status" in {
      val newTicket = Ticket("newTicket", 100, 10000)
      val tupleTicketAndStatus1 = (savedTicket, Some(oldSavedStatus))
      val tupleTicketAndStatus2 = (savedTicket, Some(newSavedStatus))
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
        whenReady(ticketMethods.findUnblockedByTariffId(10000)) { unblockedTicketsWithStatus =>
          val unblockedTickets = unblockedTicketsWithStatus map (_.ticket)
          unblockedTickets must contain (savedTicket)
          unblockedTickets must not contain savedBlockedTicket
        }
    }

    "find only unblocked tickets by eventId" in {
        whenReady(ticketMethods.findUnblockedByEventId(100)) { unblockedTicketsWithStatus =>
          val unblockedTickets = unblockedTicketsWithStatus map (_.ticket)
          unblockedTickets must contain (savedTicket)
          unblockedTickets must not contain savedBlockedTicket
        }
    }

    "block a ticket during a time laps" in {
      whenReady(ticketMethods.blockTicket(2, 1000)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findUnblockedByEventId(100)) { unblockedTicketsWithStatus =>
          val unblockedTickets = unblockedTicketsWithStatus map (_.ticket)
          unblockedTickets must not contain savedTicket
          unblockedTickets must not contain savedBlockedTicket
        }
        Thread.sleep(2000)
        whenReady(ticketMethods.findUnblockedByEventId(100)) { unblockedTicketsWithStatus =>
          val unblockedTickets = unblockedTicketsWithStatus map (_.ticket)
          unblockedTickets must contain (savedTicket)
          unblockedTickets must not contain savedBlockedTicket
        }
      }
    }

    "add bought bill for a ticket" in {
      whenReady(ticketMethods.addBoughtTicketBill
        (TicketBill(1000, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), new DateTime(), BigDecimal(10))))
      { response =>
          response mustBe 1
      }
    }

    "find bought bill by ticketId" in {
      val boughtBill = TicketBill(1000, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), new DateTime(), BigDecimal(10))
      whenReady(ticketMethods.addBoughtTicketBill(boughtBill)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findBoughtTicketBillByTicketId(1000)) { response =>
          response must contain (boughtBill)
        }
      }
    }

    "add sold bill for a ticket" in {
      whenReady(ticketMethods.addSoldTicketBill
        (TicketBill(1000, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), new DateTime(), BigDecimal(10))))
      { response =>
          response mustBe 1
      }
    }

    "find sold bill by ticketId" in {
      val soldBill = TicketBill(1000, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), new DateTime(), BigDecimal(10))
      whenReady(ticketMethods.addSoldTicketBill(soldBill)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findSoldTicketBillByTicketId(1000)) { response =>
          response must contain (soldBill)
        }
      }
    }

    "find pendding tickets" in {
      whenReady(ticketMethods.findPendingTickets) { response =>
        response must contain (savedPendingTicket)
      }
    }

    "find untreated pending tickets" in {
      whenReady(ticketMethods.findUntreatedPendingTickets) { response =>
        response must contain (savedPendingTicket)
      }
    }

    "add pending tickets" in {
      val pendingTicketToSave = PendingTicket(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 100, new DateTime,
      10, "pendingTicketToSave")
      whenReady(ticketMethods.addPendingTicket(pendingTicketToSave)) { response =>
        response mustBe 1
      }
    }

    "update pending tickets" in {
      val pendingTicketToUpdate = PendingTicket(UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"), 100, new DateTime,
      10, "pendingTicket", Some(true))
      whenReady(ticketMethods.updatePendingTicket(pendingTicketToUpdate)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findUntreatedPendingTickets) { response =>
          response must not contain savedPendingTicket
        }
      }
    }
  }
}