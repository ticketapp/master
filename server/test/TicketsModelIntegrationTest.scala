import java.util.UUID

import eventsDomain.Event
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import testsHelper.GlobalApplicationForModels
import ticketsDomain._

import scala.language.postfixOps

class TicketsModelIntegrationTest extends GlobalApplicationForModels {

  "A ticket" must {

    val savedSellableEvent = SalableEvent(eventId = 100)
    val savedTicket:Ticket = Ticket(ticketId = Some(1000L), qrCode = "savedTicket",eventId = 100,tariffId = 10000)
    val savedBlockedTicket:Ticket = Ticket(ticketId = Some(1100L), qrCode = "savedBlockedTicket",eventId = 100,tariffId = 10000)
    val oldSavedStatus = TicketStatus(ticketId = 1000,status = 'a', date = new DateTime("2015-09-22T14:00:00.000+02:00"))
    val newSavedStatus = TicketStatus(ticketId = 1000,status = 'b', date = new DateTime("2015-09-24T14:00:00.000+02:00"))
    val savedPendingTicket = PendingTicket(
      pendingTicketId = Some(1000L),
      userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
      tariffId = 10000,
      date = new DateTime("2015-09-24T14:00:00.000+02:00"),
      amount = 10,
      qrCode = "pendingTicket"
    )
    val savedBoughtBill = TicketBill(
      ticketId = 1100,
      userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
      date = new DateTime("2015-09-24T14:00:00.000+02:00"),
      amount = 10
    )
    val savedSoldBill = TicketBill(
      ticketId = 1100,
      userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
      date = new DateTime("2015-09-24T14:00:00.000+02:00"),
      amount = 10
    )

    "return id on save" in {
      val ticket = Ticket(qrCode = "test", eventId = 100, tariffId = 10000)
      whenReady(ticketMethods.save(ticket)) { ticketId =>
        ticketId mustBe 1
      }
    }

    "pass a Seq of Tuple of Ticket And Status to Seq of TicketWithStatus with the most recent status" in {
      val newTicket = Ticket(None, "newTicket", 100, 10000)
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

    "find all tickets" in {
      whenReady(ticketMethods.findAll()) { tickets =>
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
      whenReady(ticketMethods.blockTicket(2, 1000, UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"))) { response =>
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

    "find all bought bills" in {
      whenReady(ticketMethods.findAllBoughtTicketBill) {response =>
          response must contain (savedBoughtBill)
      }
    }

    "find all sold bills" in {
      whenReady(ticketMethods.findAllSoldTicketBill) {response =>
          response must contain (savedSoldBill)
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
      val pendingTicketToSave = PendingTicket(pendingTicketId = None,userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
        tariffId = 10000, date = new DateTime, amount = 10, qrCode = "pendingTicketToSave")
      whenReady(ticketMethods.addPendingTicket(pendingTicketToSave)) { response =>
        response mustBe 1
      }
    }

    "not add two pending tickets with th same qrCode" in {
      val pendingTicketToSave = PendingTicket(pendingTicketId = None,userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
        tariffId = 10000, date = new DateTime, amount = 10, qrCode = "duplicate qrCode")
      val pendingTicketDuplicate = PendingTicket(pendingTicketId = None,userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
        tariffId = 10000, date = new DateTime, amount = 10, qrCode = "duplicate qrCode")
      whenReady(ticketMethods.addPendingTicket(pendingTicketToSave)) { response =>
        response mustBe 1
        ticketMethods.addPendingTicket(pendingTicketToSave).failed.futureValue mustBe a [Exception]
      }
    }

    "update pending tickets" in {
      whenReady(ticketMethods.updatePendingTicket(savedPendingTicket.pendingTicketId, true)) { response =>
        response mustBe 1
        whenReady(ticketMethods.findUntreatedPendingTickets) { response =>
          response must not contain savedPendingTicket
        }
      }
    }

    "find pending ticket by Id" in {
      whenReady(ticketMethods.findPendingTicketById(1000)) { response =>
        response mustBe Some(response.head.copy(pendingTicketId = savedPendingTicket.pendingTicketId, qrCode = savedPendingTicket.qrCode))
      }
    }

    "find sellable event's ids" in {
      whenReady(ticketMethods.findSalableEvents) { eventIds =>
        eventIds must contain (savedSellableEvent)
      }
    }

    "add sellable event" in {
      val newSalableEvent = SalableEvent(1000)
      whenReady(ticketMethods.addSalableEvents(newSalableEvent)) { eventIds =>
        eventIds mustBe 1
      }
    }

    "find maybe salable events by containing" in {
      val expectedMaybeSalableEvent = MaybeSalableEvent(
        Event(id = Some(100),
          facebookId = None,
          isPublic = true,
          isActive = true,
          name = "notPassedEvent2",
          geographicPoint = geographicPointMethods.stringToTryPoint("45.7780684, 4.836889").get,
          description = None,
          startTime = new DateTime("2050-08-24T14:00:00.000+02:00"),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        true)

      val expectedUnSalableEvent = MaybeSalableEvent(
        Event(id = Some(5),
          facebookId = None,
          isPublic = true,
          isActive = true,
          name = "notPassedEvent",
          geographicPoint = geographicPointMethods.stringToTryPoint("48.87135809999999, 2.3521577").get,
          description = None,
          startTime = new DateTime("2040-08-24T14:00:00.000+02:00"),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        false)
      whenReady(ticketMethods.findMaybeSalableEventsByContaining("notPassed")) { events =>

        events must contain(expectedMaybeSalableEvent)
        events must contain(expectedUnSalableEvent)
      }
    }
  }
}