import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import eventsDomain.Event
import database.MyPostgresDriver.api._
import json.JsonHelper
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import ticketsDomain._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TestTicketController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime)
          VALUES(1000, 'facebookidattendeetest0', true, true, 'notPassedEvent1',
          TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00+02:00');
        INSERT INTO events(eventid, facebookId, ispublic, isactive, name, starttime)
          VALUES(100, 'facebookidattendeetest', true, true, 'notPassedEvent3',
          TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00+02:00');
        INSERT INTO events(eventid, ispublic, isactive, name, starttime)
          VALUES(5, true, true, 'notPassedEvent',
          TIMESTAMP WITH TIME ZONE '2050-08-24 14:00:00+02:00');

        INSERT INTO tariffs(tariffId, denomination, price, startTime, endTime, eventId)
          VALUES(10000, 'test', 10, TIMESTAMP WITH TIME ZONE '2040-08-24 14:00:00+02:00',
          TIMESTAMP WITH TIME ZONE '2040-09-24 14:00:00+02:00', 100);

        INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES(1000, 'savedTicket', 100, 10000);
        INSERT INTO tickets(ticketId, qrCode, eventId, tariffId) VALUES(1100, 'savedBlockedTicket', 100, 10000);

        INSERT INTO pendingTickets(pendingTicketId, userId, tariffId, date, amount, qrCode)
          VALUES(1000, '077f3ea6-2272-4457-a47e-9e9111108e44', 10000,
          TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00', 10, 'pendingTicket');

        INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES(1000, 1000, 'a',
          TIMESTAMP WITH TIME ZONE '2015-09-22 14:00:00+02:00');
        INSERT INTO ticketStatuses(id, ticketId, status, date) VALUES(1100, 1000, 'b',
          TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00');

        INSERT INTO boughtTicketBills(billId, ticketId, userId, date, amount) VALUES
         (1000, 1100, '077f3ea6-2272-4457-a47e-9e9111108e44', TIMESTAMP WITH TIME ZONE  '2015-09-24 14:00:00+02:00', 10);
       
        INSERT INTO soldTicketBills(billId, ticketId, userId, date, amount) VALUES
         (1000, 1100, '077f3ea6-2272-4457-a47e-9e9111108e44', TIMESTAMP WITH TIME ZONE '2015-09-24 14:00:00+02:00', 10);

        INSERT INTO salableEvents(eventId) VALUES (100);"""),
      5.seconds)
  }

  val savedSalableEvent = SalableEvent(eventId = 100)
  val savedPendingTicket = PendingTicket(
    pendingTicketId = Some(1000L),
    userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"),
    tariffId = 10000,
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10,
    qrCode = "pendingTicket")
  val savedTicket:Ticket = Ticket(ticketId = Some(1000L), qrCode = "savedTicket",eventId = 100,tariffId = 10000)
  val newSavedStatus = TicketStatus(ticketId = 1000,status = 'b', date = new DateTime("2015-09-24T14:00:00.000+02:00"))
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

  "TicketController" should {

    "get all salable events" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findSalableEvents()))
      val validatedJsonSalableEvents: JsResult[Seq[SalableEvent]] =
        contentAsJson(info).validate[Seq[SalableEvent]](JsonHelper.readSalableEventReads)

      val expectedTicket = validatedJsonSalableEvents match {
        case events: JsSuccess[Seq[SalableEvent]] =>
          events.get
        case error: JsError =>
          throw new Exception
      }

      expectedTicket must contain(savedSalableEvent)
    }

    "add a salable event" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.addSalableEvents(1000))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "propose new ticket" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.proposeTicket(10000,  10, "newProposition"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "block a ticket for a user" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.blockTicketForUser(10000))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "add a ticket to sell" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.addTicketToSale("newTicketToSell", 100, 10000))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "find pending tickets " in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findPendingTickets())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val validatedJsonTicketsEvents: JsResult[Seq[PendingTicket]] =
        contentAsJson(info).validate[Seq[PendingTicket]](JsonHelper.readPendingTicketReads)

      validatedJsonTicketsEvents match {
        case tickets: JsSuccess[Seq[PendingTicket]] =>
          tickets.get must contain (savedPendingTicket)
        case error: JsError =>
          throw new Exception
      }
    }

    "accept pending ticket " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.acceptPendingTicket(savedPendingTicket.pendingTicketId.get))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "reject pending ticket " in {
      val Some(info) = route(
        FakeRequest(ticketsDomain.routes.TicketController.rejectPendingTicket(savedPendingTicket.pendingTicketId.get))
          .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(info).toInt mustEqual 1
    }

    "find all tickets with status " in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findTicketsWithStatus())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val validatedJsonTicketsEvents: JsResult[Seq[TicketWithStatus]] =
        contentAsJson(info).validate[Seq[TicketWithStatus]](JsonHelper.readTicketWithStatusReads)

      validatedJsonTicketsEvents match {
        case tickets: JsSuccess[Seq[TicketWithStatus]] =>
          tickets.get must contain (TicketWithStatus(savedTicket, Some(newSavedStatus)))
        case error: JsError =>
          throw new Exception
      }
    }

    "find bought bills " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findBoughtBills())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      val validatedJsonTicketsEvents: JsResult[Seq[TicketBill]] =
        contentAsJson(info).validate[Seq[TicketBill]](JsonHelper.readTicketBillReads)

      validatedJsonTicketsEvents match {
        case ticketBills: JsSuccess[Seq[TicketBill]] =>
          ticketBills.get must contain (savedBoughtBill)
        case error: JsError =>
          throw new Exception
      }
    }

    "find sold bills " in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findSoldBills())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))
      val validatedJsonTicketsEvents: JsResult[Seq[TicketBill]] =
        contentAsJson(info).validate[Seq[TicketBill]](JsonHelper.readTicketBillReads)

      validatedJsonTicketsEvents match {
        case ticketBills: JsSuccess[Seq[TicketBill]] =>
          ticketBills.get must contain (savedSoldBill)
        case error: JsError =>
          throw new Exception
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

      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findMaybeSalableEventsByContaining("notPassed"))
      )
      val validatedJsonMaybeSalableEvents: JsResult[Seq[MaybeSalableEvent]] =
        contentAsJson(info).validate[Seq[MaybeSalableEvent]](JsonHelper.readMaybeSalableEventReads)

      val maybeSalableEventsResult = validatedJsonMaybeSalableEvents match {
        case maybeSalableEvent: JsSuccess[Seq[MaybeSalableEvent]] =>
          maybeSalableEvent.get
        case error: JsError =>
          throw new Exception
      }

      maybeSalableEventsResult must contain(expectedMaybeSalableEvent)
      maybeSalableEventsResult must contain(expectedUnSalableEvent)
    }
  }
}
