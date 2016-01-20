package ticketsDomain

import play.api.mvc._

class TicketController extends Controller {

  def indexAdmin = Action {
    Ok(views.html.admin.indexAdmin())
  }
//
//  def createOrder(totalPrice: Int): Long = {
//    Order.save(totalPrice)
//  }

//  def addTicketToBlockedTariffs(tariffId: Long): Option[Long] = {
//    try {
//      DB.withConnection { implicit connection =>
//        SQL("INSERT INTO blockedTariffs(tariffId) VALUES ({tariffId})").on(
//          'tariffId -> tariffId
//        ).executeInsert()
//      }
//    } catch {
//      case e: Exception => throw new DAOException("Cannot create an entry in blockedTariffs table: " + e.getMessage)
//    }
//  }

  def buyTicket = Action {
/*    val orderId = Order.save(10)
    AccountingController.createBankLine(10, true, null, orderId)
    AccountingController.createAccount63Line("TVA", 10, orderId) match {
      case None =>
      case Some(account63Id) => AccountingController.createAccount4686Line(false, 10, account63Id)
    }
    AccountingController.createAccount627LineAndBankLine("Pourcentage Banque", 10, orderId)
    AccountingController.createAccount60LineAndAccount403Line("Pourcentage Orga", 10, orderId)*/
//    Redirect(routes.Admin.indexAdmin())
    Ok
  }
}