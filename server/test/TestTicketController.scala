import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import json.JsonHelper
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.test.FakeRequest
import testsHelper.GlobalApplicationForControllers
import ticketsDomain.{PendingTicket, SellableEvent}

class TestTicketController extends GlobalApplicationForControllers {
  sequential

  val savedSellableEvent = SellableEvent(eventId = 100)
  val savedPendingTicket = PendingTicket(
    pendingTicketId = Some(1000L),
    userId = UUID.fromString("a4aea509-1002-47d0-b55c-593c91cb32ae"),
    tariffId = 10000,
    date = new DateTime("2015-09-24T14:00:00.000+02:00"),
    amount = 10,
    qrCode = "pendingTicket"
  )

  "TicketController" should {

    "get all sellable events" in {
      val Some(info) = route(FakeRequest(ticketsDomain.routes.TicketController.findSellableEvents()))
      val validatedJsonSellableEvents: JsResult[Seq[SellableEvent]] =
        contentAsJson(info).validate[Seq[SellableEvent]](JsonHelper.readSellableEventReads)
      validatedJsonSellableEvents match {
        case events: JsSuccess[Seq[SellableEvent]] =>
          events.get must contain (savedSellableEvent)
        case error: JsError =>
          Logger.error("get all sellable events:" + error)
          error mustEqual 0
      }
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

    "accept pending ticket " in {
      val Some(info) = route(FakeRequest(
        ticketsDomain.routes.TicketController.acceptPendingTicket(savedPendingTicket.pendingTicketId.get))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo)
      )
      contentAsString(info).toInt mustEqual 1
    }
  }
}