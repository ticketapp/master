package tariffsDomain

import javax.inject.Inject

import application.User
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import json.JsonHelper._
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class TariffController @Inject()(val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 val tariffMethods: TariffMethods)
  extends Silhouette[User, CookieAuthenticator] {


  def findTariffsByEventId(eventId: Long) = Action.async {
    tariffMethods.findByEventId(eventId) map { tariffs =>
      Ok(Json.toJson(tariffs))
    } recover { case t: Throwable =>
      Logger.error("TicketController.findSalableEvents: ", t)
      InternalServerError("TicketController.findSalableEvents: " + t.getMessage)
    }
  }

  def save(denomination: String, eventId: Long, startTime: String, endTime: String, price: Double) = Action.async {
    val tariff = Tariff(None, denomination, eventId, new DateTime(startTime), new DateTime(endTime), price)
    tariffMethods.save(tariff) map { response =>
      Ok(Json.toJson(response))
    } recover { case t: Throwable =>
      Logger.error("TicketController.addSalableEvents: ", t)
      InternalServerError("TicketController.addSalableEvents: " + t.getMessage)
    }
  }
}