package ticketsDomain

import javax.inject.Inject

import application.User
import com.mohiva.play.silhouette.api.{Silhouette, Environment}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import json.JsonHelper._
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Success, Failure}

class TicketController @Inject()(val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 val ticketMethods: TicketMethods)
  extends Silhouette[User, CookieAuthenticator] {


  def findSellableEvents() = Action.async {
    ticketMethods.findSellableEvents map { sellableEvents =>
        Ok(Json.toJson(sellableEvents))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findSellableEvents: ", t)
      InternalServerError("TicketController.findSellableEvents: " + t.getMessage)
    }
  }
  def proposeTicket(tariffId: Long, amount: BigDecimal, qrCode: String) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    val pendingTicket = PendingTicket(None, userId, tariffId, new DateTime(), amount, qrCode)
    ticketMethods.addPendingTicket(pendingTicket) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.proposeTicket: ", t)
      InternalServerError("TicketController.proposeTicket: " + t.getMessage)
    }
  }

  def blockTicketForUser(tariffId: Long) = SecuredAction.async { implicit request =>
    val userId = request.identity.uuid
    ticketMethods.findAllByTariffId(tariffId) flatMap { tickets =>
      tickets.find(t => t.ticketStatus.nonEmpty && t.ticketStatus.get.status == 'b') match {
        case Some(withStatus) =>
          withStatus.ticket.ticketId match {
            case Some(ticketId) =>
              ticketMethods.blockTicket(900, ticketId, userId) map { response =>
                  Ok(Json.toJson(response))
              }
            case _ =>
              Logger.error("TicketController.buyTicket: no id for a found ticket")
              Future(InternalServerError("TicketController.buyTicket: no id for a found ticket"))
          }

        case _ =>
          Future(NotFound("no ticket for this tariff"))
      }
    }
  }

  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long) = SecuredAction.async { implicit request =>
    val newTicket = Ticket(qrCode = qrCode, eventId = eventId, tariffId = tariffId)
    ticketMethods.save(newTicket) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.addTicketToSale for :" + eventId, t)
      InternalServerError("TicketController.addTicketToSale: " + t.getMessage)
    }
  }

  def acceptPendingTicket(pendingTicketId: Long) = SecuredAction.async { implicit request =>
    ticketMethods.updatePendingTicket(Some(pendingTicketId), true) map { response =>
      Ok(Json.toJson(response))
    }
  }

  def rejectTicket = ???

  def findTicketsWithStatus = ???

  def findPenddingTickets = ???

  def findSellBills = ???

  def findBoughtBills = ???

}