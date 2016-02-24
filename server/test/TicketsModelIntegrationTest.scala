import java.util.UUID

import com.vividsolutions.jts.geom.Geometry
import eventsDomain.Event
import database.MyPostgresDriver.api._
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures._
import testsHelper.GlobalApplicationForModelsIntegration
import ticketsDomain._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TicketsModelIntegrationTest extends GlobalApplicationForModelsIntegration {

  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime)
          VALUES(100, 'facebookidattendeetest', true, true, 'notPassedEvent3', TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00+02:00');

        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime)
          VALUES(1000, 'facebookidattendeetest2', true, true, 'notPassedEvent4', TIMESTAMP WITH TIME ZONE '2030-08-24 14:00:00+02:00');
       INSERT INTO events(eventid, ispublic, isactive, name, starttime)
         VALUES(5, true, true, 'notPassedEvent', TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00+02:00');

        INSERT INTO tariffs(tariffId, denomination, price, startTime, endTime, eventId)
          VALUES(10000, 'test', 10, TIMESTAMP WITH TIME ZONE '2040-08-24T14:00:00.000+02:00',
          TIMESTAMP WITH TIME ZONE '2040-09-24T14:00:00.000+02:00', 100);
          
        INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES(1000, 'savedTicket', 100, 10000);
        INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES(1100, 'savedBlockedTicket', 100, 10000);

        INSERT INTO blockedTickets(id, ticketId, expirationDate, userId)
          VALUES(1000, 1100, TIMESTAMP WITH TIME ZONE '2055-09-24 14:00:00+02:00', '077f3ea6-2272-4457-a47e-9e9111108e44');

        INSERT INTO pendingTickets(pendingTicketId, userId, tariffId, date, amount, qrCode)
          VALUES(1000, '077f3ea6-2272-4457-a47e-9e9111108e44', 10000, TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00', 10, 'pendingTicket');
                                   
        INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES(1000, 1000, 'a', TIMESTAMP WITH TIME ZONE '2015-09-22 14:00:00+02:00');
        INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES(1100, 1000, 'b', TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00');
                                   
         INSERT INTO boughtTicketBills(billId, ticketId, userId, date, amount) VALUES
           (1000, 1100, '077f3ea6-2272-4457-a47e-9e9111108e44', TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00', 10);
                                          
         INSERT INTO soldTicketBills(billId, ticketId, userId, date, amount) VALUES
           (1000, 1100, '077f3ea6-2272-4457-a47e-9e9111108e44', TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00', 10);
                                   
         INSERT INTO salableEvents(eventId) VALUES (100);"""),
      2.seconds)
  }

  val savedSalableEvent = SalableEvent(eventId = 100)
  val savedTicket:Ticket = Ticket(ticketId = Some(1000L), qrCode = "savedTicket",eventId = 100,tariffId = 10000)
  val savedBlockedTicket:Ticket = Ticket(ticketId = Some(1100L), qrCode = "savedBlockedTicket",eventId = 100,tariffId = 10000)
  val oldSavedStatus = TicketStatus(ticketId = 1000,status = 'a', date = new DateTime("2015-09-22T14:00:00.000+02:00"))
  val newSavedStatus = TicketStatus(ticketId = 1000,status = 'b', date = new DateTime("2015-09-24T14:00:00.000+02:00"))
  val savedPendingTicket = PendingTicket(
    pendingTicketId = Some(1000L),
    userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
    tariffId = 10000,
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10,
    qrCode = "pendingTicket")
  val savedBoughtBill = TicketBill(
    ticketId = 1100,
    userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10)
  val savedSoldBill = TicketBill(
    ticketId = 1100,
    userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10)

  "A ticket" must {

    "return its id when saved" in {
      val ticket = Ticket(qrCode = "test", eventId = 100, tariffId = 10000)
      whenReady(ticketMethods.save(ticket)) { ticketId =>
        ticketId mustBe 1
      }
    }

    "transform a sequence of tuple of Ticket And Status to a Seq of TicketWithStatus with the most recent status" in {
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
      whenReady(ticketMethods.blockTicket(2, 1000, UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))) { response =>
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
        (TicketBill(1000, UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"), new DateTime(), BigDecimal(10))))
      { response =>
          response mustBe 1
      }
    }

    "find bought bill by ticketId" in {
      val boughtBill = TicketBill(1000, UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"), new DateTime(), BigDecimal(10))
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
        (TicketBill(1000, UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"), new DateTime(), BigDecimal(10))))
      { response =>
          response mustBe 1
      }
    }

    "find sold bill by ticketId" in {
      val soldBill = TicketBill(1000, UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"), new DateTime(), BigDecimal(10))
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
      val pendingTicketToSave = PendingTicket(pendingTicketId = None,userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
        tariffId = 10000, date = new DateTime, amount = 10, qrCode = "pendingTicketToSave")
      whenReady(ticketMethods.addPendingTicket(pendingTicketToSave)) { response =>
        response mustBe 1
      }
    }

    "not add two pending tickets with th same qrCode" in {
      val pendingTicketToSave = PendingTicket(pendingTicketId = None,userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
        tariffId = 10000, date = new DateTime, amount = 10, qrCode = "duplicate qrCode")

      whenReady(ticketMethods.addPendingTicket(pendingTicketToSave)) { response =>
        response mustBe 1
        ticketMethods.addPendingTicket(pendingTicketToSave).failed.futureValue mustBe an [Exception]
      }
    }

    "update pending tickets" in {
      whenReady(ticketMethods.updatePendingTicket(savedPendingTicket.pendingTicketId, isValidate = true)) { response =>
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

    "find salable event's ids" in {
      whenReady(ticketMethods.findSalableEvents) { eventIds =>
        eventIds must contain (savedSalableEvent)
      }
    }

    "add salable event" in {
      val newSalableEvent = SalableEvent(1000)
      whenReady(ticketMethods.addSalableEvents(newSalableEvent)) { response =>
        response mustBe 1
      }
    }

    "find salable events by geographicPoint" in {
      val expectedMaybeSalableEvent = MaybeSalableEvent(
        Event(
          id = Some(100),
          facebookId = Some("facebookidattendeetest"),
          isPublic = true,
          isActive = true,
          name = "notPassedEvent3",
          geographicPoint = geographicPointMethods.stringToTryPoint("-84, 30").get,
          description = None,
          startTime = new DateTime("2050-08-24T14:00:00.000+02:00"),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        isSalable = true
      )
      val geographicPoint = geographicPointMethods.stringToTryPoint("-84, 30").get
      val offset = 0
      val numberToReturn = 10

      whenReady(ticketMethods.findMaybeSalableEventsNear(geographicPoint: Geometry, offset: Int, numberToReturn: Int)) {
        events =>

        events must contain(expectedMaybeSalableEvent)
      }

    }

    "find maybe salable events by containing" in {
      val expectedMaybeSalableEvent = MaybeSalableEvent(
        Event(
          id = Some(100),
          facebookId = Some("facebookidattendeetest"),
          isPublic = true,
          isActive = true,
          name = "notPassedEvent3",
          geographicPoint = geographicPointMethods.stringToTryPoint("-84, 30").get,
          description = None,
          startTime = new DateTime("2050-08-24T14:00:00.000+02:00"),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        isSalable = true
      )

      val expectedUnSalableEvent = MaybeSalableEvent(
        Event(
          id = Some(5),
          facebookId = None,
          isPublic = true,
          isActive = true,
          name = "notPassedEvent",
          geographicPoint = geographicPointMethods.stringToTryPoint("-84, 30").get,
          description = None,
          startTime = new DateTime("2050-08-24T14:00:00.000+02:00"),
          endTime = None,
          ageRestriction = 16,
          tariffRange = None,
          ticketSellers = None,
          imagePath = None),
        isSalable = false
      )
      whenReady(ticketMethods.findMaybeSalableEventsContaining("notPassed")) { events =>

        events must contain(expectedMaybeSalableEvent)
        events must contain(expectedUnSalableEvent)
      }
    }
  }
}