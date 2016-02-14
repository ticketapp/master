import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import eventsDomain.Event
import json.JsonHelper
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import ticketsDomain._

class TestTicketController extends GlobalApplicationForControllers {
  sequential

  val savedSalableEvent = SalableEvent(eventId = 100)
  val savedPendingTicket = PendingTicket(
    pendingTicketId = Some(1000L),
    userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
    tariffId = 10000,
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10,
    qrCode = "pendingTicket"
  )
  val savedTicket:Ticket = Ticket(ticketId = Some(1000L), qrCode = "savedTicket",eventId = 100,tariffId = 10000)
  val newSavedStatus = TicketStatus(ticketId = 1000,status = 'b', date = new DateTime("2015-09-24T14:00:00.000+02:00"))

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

  "TicketController" should {

    "get all salable events" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findSalableEvents()))
      val validatedJsonSalableEvents: JsResult[Seq[SalableEvent]] =
        contentAsJson(info).validate[Seq[SalableEvent]](JsonHelper.readSalableEventReads)
      validatedJsonSalableEvents match {
        case events: JsSuccess[Seq[SalableEvent]] =>
          events.get must contain (savedSalableEvent)
        case error: JsError =>
          Logger.error("get all salable events:" + error)
          error mustEqual 0
      }
    }
    "add a salable event" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.addSalableEvents(1000))
      .withAuthenticator[CookieAuthenticator](identity.loginInfo))
        contentAsString(info).toInt mustEqual 1
    }

    "propose new ticket" in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.proposeTicket(10000,  10, "newProposition"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }

    "block a ticket for a user" in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.blockTicketForUser(10000))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }

    "add a ticket to sell" in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.addTicketToSale("newTicketToSell", 100, 10000))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }

    "find pending tickets " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findPendingTickets())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      val validatedJsonTicketsEvents: JsResult[Seq[PendingTicket]] =
        contentAsJson(info).validate[Seq[PendingTicket]](JsonHelper.readPendingTicketReads)
      validatedJsonTicketsEvents match {
        case tickets: JsSuccess[Seq[PendingTicket]] =>
          tickets.get must contain (savedPendingTicket)
        case error: JsError =>
          Logger.error("get all salable events:" + error)
          error mustEqual 0
      }
    }

    "accept pending ticket " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.acceptPendingTicket(savedPendingTicket.pendingTicketId.get))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }

    "reject pending ticket " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.rejectPendingTicket(savedPendingTicket.pendingTicketId.get))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }

    "find all tickets with status " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findTicketsWithStatus())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      val validatedJsonTicketsEvents: JsResult[Seq[TicketWithStatus]] =
        contentAsJson(info).validate[Seq[TicketWithStatus]](JsonHelper.readTicketWithStatusReads)
      validatedJsonTicketsEvents match {
        case tickets: JsSuccess[Seq[TicketWithStatus]] =>
          tickets.get must contain (TicketWithStatus(savedTicket, Some(newSavedStatus)))
        case error: JsError =>
          Logger.error("get all salable events:" + error)
          error mustEqual 0
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
          Logger.error("get all salable events:" + error)
          error mustEqual 0
      }
    }

    "find sold bills " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findSoldBills())
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      val validatedJsonTicketsEvents: JsResult[Seq[TicketBill]] =
        contentAsJson(info).validate[Seq[TicketBill]](JsonHelper.readTicketBillReads)
      validatedJsonTicketsEvents match {
        case ticketBills: JsSuccess[Seq[TicketBill]] =>
          ticketBills.get must contain (savedSoldBill)
        case error: JsError =>
          Logger.error("get all salable events:" + error)
          error mustEqual 0
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

      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.findMaybeSalableEventsByContaining("notPassed"))
      )
      val validatedJsonMaybeSalableEvents: JsResult[Seq[MaybeSalableEvent]] =
        contentAsJson(info).validate[Seq[MaybeSalableEvent]](JsonHelper.readMaybeSalableEventReads)

      val maybeSalableEventsResult = validatedJsonMaybeSalableEvents match {
        case maybeSalableEvent: JsSuccess[Seq[MaybeSalableEvent]] =>
          maybeSalableEvent.get
        case error: JsError =>
          Logger.error("get all salable events:" + error)
          throw new Exception
      }

      maybeSalableEventsResult must contain(expectedMaybeSalableEvent)
      maybeSalableEventsResult must contain(expectedUnSalableEvent)
    }


  }
}