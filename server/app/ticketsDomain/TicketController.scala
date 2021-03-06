package ticketsDomain

import javax.inject.Inject

import addresses.SearchGeographicPoint
import application.{Administrator, User}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.vividsolutions.jts.geom.Geometry
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.control.NonFatal

class TicketController @Inject()(val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 val ticketMethods: TicketMethods,
                                 val geographicPointMethods: SearchGeographicPoint)
  extends Silhouette[User, CookieAuthenticator] {


  def findSalableEvents() = Action.async {
    ticketMethods.findSalableEvents map { salableEvents =>
        Ok(Json.toJson(salableEvents))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findSalableEvents: ", t)
      InternalServerError("TicketController.findSalableEvents: " + t.getMessage)
    }
  }

  def findMaybeSalableEventsContaining(pattern: String) = Action.async {
    ticketMethods.findMaybeSalableEventsContaining(pattern) map { maybeSalableEvents =>
        Ok(Json.toJson(maybeSalableEvents))
    } recover { case NonFatal(e) =>
      Logger.error(this.getClass + e.getStackTrace.apply(1).getMethodName, e)
      InternalServerError(this.getClass + "findMaybeSalableEventsByContaining: " + e.getMessage)
    }
  }


  def findMaybeSalableEventsNear(geographicPointString: String, offset: Int, numberToReturn: Int) = Action.async {
    val geographicPoint = geographicPointMethods.stringToTryPoint(geographicPointString).get
    ticketMethods.findMaybeSalableEventsNear(geographicPoint: Geometry, offset: Int, numberToReturn: Int) map {
      maybeSalableEvents =>
        Ok(Json.toJson(maybeSalableEvents))
    } recover { case NonFatal(e) =>
      Logger.error(this.getClass + e.getStackTrace.apply(1).getMethodName, e)
      InternalServerError(this.getClass + "findMaybeSalableEventsByContaining: " + e.getMessage)
    }
  }

  def addSalableEvents(eventId: Long) = SecuredAction(Administrator()).async {
    ticketMethods.addSalableEvents(SalableEvent(eventId)) map { response =>
        Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.addSalableEvents: ", t)
      InternalServerError("TicketController.addSalableEvents: " + t.getMessage)
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

  def addTicketToSale(qrCode: String, eventId: Long, tariffId: Long) = SecuredAction(Administrator()).async {
    implicit request =>
    val newTicket = Ticket(qrCode = qrCode, eventId = eventId, tariffId = tariffId)
    ticketMethods.save(newTicket) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.addTicketToSale for :" + eventId, t)
      InternalServerError("TicketController.addTicketToSale: " + t.getMessage)
    }
  }

  def findPendingTickets = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.findPendingTickets map { pendingTickets =>
      Ok(Json.toJson(pendingTickets))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findPendingTickets:", t)
      InternalServerError("TicketController.findPendingTickets: " + t.getMessage)
    }
  }

  def acceptPendingTicket(pendingTicketId: Long) = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.updatePendingTicket(Some(pendingTicketId), isValidate = true) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.acceptPendingTicket for :" + pendingTicketId, t)
      InternalServerError("TicketController.acceptPendingTicket: " + t.getMessage)
    }
  }

  def rejectPendingTicket(pendingTicketId: Long) = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.updatePendingTicket(Some(pendingTicketId), isValidate = false) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.rejectPendingTicket for :" + pendingTicketId, t)
      InternalServerError("TicketController.rejectPendingTicket: " + t.getMessage)
    }
  }

  def findTicketsWithStatus = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.findAll() map { tickets =>
      Ok(Json.toJson(tickets))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findTicketsWithStatus :", t)
      InternalServerError("TicketController.findTicketsWithStatus: " + t.getMessage)
    }
  }

  def findBoughtBills = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.findAllBoughtTicketBill map {bills =>
      Ok(Json.toJson(bills))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findBoughtBills :", t)
      InternalServerError("TicketController.findBoughtBills: " + t.getMessage)
    }
  }

  def findSoldBills = SecuredAction(Administrator()).async { implicit request =>
    ticketMethods.findAllSoldTicketBill map {bills =>
      Ok(Json.toJson(bills))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findSoldBills :", t)
      InternalServerError("TicketController.findSoldBills: " + t.getMessage)
    }
  }

}