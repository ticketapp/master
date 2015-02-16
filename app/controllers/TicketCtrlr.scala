package controllers


import anorm._
import play.api.db.DB
import play.api.Play.current
import play.api.mvc._
import models._
import play.api.libs.json.Json

object TicketCtrlr extends Controller {

  def indexAdmin = Action {
    Ok(views.html.admin.indexAdmin())
  }

  def createOrder(totalPrice: Int): Long = {
    Order.save(totalPrice)
  }

  def addTicketToBlockedTariffs(tariffId: Long): Option[Long] = {
    try {
      DB.withConnection { implicit connection =>
        SQL("INSERT INTO blockedTariffs(tariffId) VALUES ({tariffId})").on(
          'tariffId -> tariffId
        ).executeInsert()
      }
    } catch {
      case e: Exception => throw new DAOException("Cannot create an entry in blockedTariffs table: " + e.getMessage)
    }
  }

  def buyTicket = Action {
    val orderId = Order.save(10)
    AccountingCtrlr.createBankLine(10, true, null, orderId)
    AccountingCtrlr.createAccount63Line("TVA", 10, orderId) match {
      case None =>
      case Some(account63Id) => AccountingCtrlr.createAccount4686Line(false, 10, account63Id)
    }
    AccountingCtrlr.createAccount627LineAndBankLine("Pourcentage Banque", 10, orderId)
    AccountingCtrlr.createAccount60LineAndAccount403Line("Pourcentage Orga", 10, orderId)
    Redirect(routes.Admin.indexAdmin())
  }
}